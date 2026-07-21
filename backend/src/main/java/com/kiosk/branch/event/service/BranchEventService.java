package com.kiosk.branch.event.service;

import com.kiosk.branch.event.dto.BranchEventResponse;
import com.kiosk.branch.event.dto.FlavorOptionResponse;
import com.kiosk.branch.event.dto.SelectFlavorRequest;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventBranchFlavor;
import com.kiosk.domain.event.EventBranchFlavorRepository;
import com.kiosk.domain.event.EventRepository;
import com.kiosk.domain.event.EventStatus;
import com.kiosk.domain.event.EventType;
import com.kiosk.domain.inventory.BranchInventory;
import com.kiosk.domain.inventory.BranchInventoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 본점이 만든 "상품(맛) 할인"(FLAVOR_DISCOUNT) 이벤트에 대해, 실제로 어느 맛에 그 할인을 붙일지는
// 지점이 고른다 (event_branch_flavor). 쿠폰형(COUPON) 이벤트는 지점이 관여할 게 없어서 목록에 나오지 않는다.
@Service
@RequiredArgsConstructor
@Transactional
public class BranchEventService {

    private final EventRepository eventRepository;
    private final EventBranchFlavorRepository eventBranchFlavorRepository;
    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public List<BranchEventResponse> list(Long branchId) {
        List<FlavorOptionResponse> flavorOptions = flavorOptionsOf(branchId);
        return eventRepository.findByEventTypeAndStatus(EventType.FLAVOR_DISCOUNT, EventStatus.ACTIVE).stream()
                .map(event -> toResponse(event, branchId, flavorOptions))
                .toList();
    }

    public BranchEventResponse selectFlavor(Long branchId, Long eventId, SelectFlavorRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        if (event.getEventType() != EventType.FLAVOR_DISCOUNT) {
            throw new IllegalArgumentException("상품(맛) 할인 이벤트가 아닙니다.");
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."));
        BranchInventory inventory = branchInventoryRepository.findByBranch_BranchIdAndFlavor_FlavorId(branchId, request.flavorId())
                .orElseThrow(() -> new IllegalArgumentException("우리 지점이 취급하지 않는 맛입니다."));

        // 지점당 이벤트 하나엔 맛 하나만 - 기존 선택이 있으면 지우고 새로 넣는다
        eventBranchFlavorRepository.deleteByEvent_EventIdAndBranch_BranchId(eventId, branchId);
        eventBranchFlavorRepository.save(EventBranchFlavor.builder()
                .event(event)
                .branch(branch)
                .flavor(inventory.getFlavor())
                .build());

        return toResponse(event, branchId, flavorOptionsOf(branchId));
    }

    private List<FlavorOptionResponse> flavorOptionsOf(Long branchId) {
        return branchInventoryRepository.findByBranch_BranchIdOrderByFlavor_FlavorNameAsc(branchId).stream()
                .map(inventory -> new FlavorOptionResponse(inventory.getFlavor().getFlavorId(), inventory.getFlavor().getFlavorName()))
                .toList();
    }

    private BranchEventResponse toResponse(Event event, Long branchId, List<FlavorOptionResponse> flavorOptions) {
        EventBranchFlavor selected = eventBranchFlavorRepository
                .findByEvent_EventIdAndBranch_BranchId(event.getEventId(), branchId)
                .orElse(null);
        return new BranchEventResponse(
                event.getEventId(),
                event.getEventName(),
                event.getBenefitType().name(),
                event.getDiscountRate(),
                event.getDiscountAmount(),
                event.getStartAt(),
                event.getEndAt(),
                selected != null ? selected.getFlavor().getFlavorId() : null,
                selected != null ? selected.getFlavor().getFlavorName() : null,
                flavorOptions
        );
    }
}
