package com.kiosk.kiosk.event.service;

import com.kiosk.domain.event.BenefitType;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventBranchFlavor;
import com.kiosk.domain.event.EventBranchFlavorRepository;
import com.kiosk.domain.event.EventRepository;
import com.kiosk.domain.event.EventStatus;
import com.kiosk.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 지금 시각 기준으로 실제 진행 중인 맛 할인/사이즈업 이벤트를 판단한다. 키오스크 메뉴 표시(MenuService)와
// 결제 금액 계산(OrderService)이 같은 기준을 쓰도록 공용으로 둔다.
// 맛 관련 이벤트는 세 경로가 있다: MONTHLY_FLAVOR(본점이 맛을 직접 지정, 할인 없이 사이즈업만) /
// HQ_FLAVOR_DISCOUNT(본점이 맛과 할인을 둘 다 직접 지정) - 이 둘은 전 지점 자동 적용 /
// FLAVOR_DISCOUNT(본점은 할인값만 정하고 지점이 event_branch_flavor로 맛을 선택).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KioskFlavorDiscountService {

    private static final List<EventType> HQ_WIDE_FLAVOR_EVENT_TYPES = List.of(
            EventType.MONTHLY_FLAVOR, EventType.HQ_FLAVOR_DISCOUNT);

    private final EventRepository eventRepository;
    private final EventBranchFlavorRepository eventBranchFlavorRepository;

    public Map<Long, Event> activeDiscountsByFlavor(Long branchId) {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Event> result = new HashMap<>();

        for (Event event : eventRepository.findByEventTypeInAndStatus(HQ_WIDE_FLAVOR_EVENT_TYPES, EventStatus.ACTIVE)) {
            if (event.getFlavor() != null && isCurrentlyActive(event, now)) {
                result.put(event.getFlavor().getFlavorId(), event);
            }
        }

        if (branchId != null) {
            for (EventBranchFlavor mapping : eventBranchFlavorRepository.findByBranch_BranchId(branchId)) {
                Event event = mapping.getEvent();
                if (event.getEventType() == EventType.FLAVOR_DISCOUNT && isCurrentlyActive(event, now)) {
                    result.put(mapping.getFlavor().getFlavorId(), event);
                }
            }
        }
        return result;
    }


    // 이 상품으로 사이즈업 도착하는(toProduct 기준) 진행 중인 MONTHLY_FLAVOR 이벤트의 사이즈업 정보 -
    // 결제 금액 재계산에 쓰인다 (사이즈업은 MONTHLY_FLAVOR에 딸린 선택 항목이라 SIZE_UP이라는 별도 타입은 없다).
    public Optional<Event> activeSizeUpEventTo(Long toProductId) {
        if (toProductId == null) return Optional.empty();
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findByEventTypeAndStatus(EventType.MONTHLY_FLAVOR, EventStatus.ACTIVE).stream()
                .filter(event -> isCurrentlyActive(event, now))
                .filter(event -> event.getSizeUpToProduct() != null
                        && toProductId.equals(event.getSizeUpToProduct().getProductId()))
                .findFirst();
    }

    // 한 주문 항목에 여러 맛이 담겨도, 할인이 중복 적용되지 않도록 그중 가장 큰 할인 하나만 적용한다.
    public int resolveDiscount(Map<Long, Event> activeDiscounts, List<Long> flavorIds, int itemBaseTotal) {
        if (flavorIds == null || flavorIds.isEmpty() || activeDiscounts.isEmpty()) {
            return 0;
        }
        int maxDiscount = 0;
        for (Long flavorId : flavorIds) {
            Event event = activeDiscounts.get(flavorId);
            if (event != null) {
                maxDiscount = Math.max(maxDiscount, discountOf(event, itemBaseTotal));
            }
        }
        return maxDiscount;
    }

    private int discountOf(Event event, int baseAmount) {
        if (event.getBenefitType() == BenefitType.DISCOUNT_RATE) {
            return event.getDiscountRate() == null ? 0
                    : (int) Math.round(baseAmount * event.getDiscountRate().doubleValue() / 100);
        }
        return event.getDiscountAmount() != null ? event.getDiscountAmount() : 0;
    }

    private boolean isCurrentlyActive(Event event, LocalDateTime now) {
        return event.getStatus() == EventStatus.ACTIVE
                && !now.isBefore(event.getStartAt())
                && now.isBefore(event.getEndAt());
    }
}
