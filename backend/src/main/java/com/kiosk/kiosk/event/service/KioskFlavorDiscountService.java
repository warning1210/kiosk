package com.kiosk.kiosk.event.service;

import com.kiosk.domain.event.BenefitType;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventBranchFlavor;
import com.kiosk.domain.event.EventBranchFlavorRepository;
import com.kiosk.domain.event.EventStatus;
import com.kiosk.domain.event.EventType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 지점이 event_branch_flavor로 선택해둔 "상품(맛) 할인" 이벤트 중, 지금 시각 기준으로 실제
// 진행 중인 것만 flavorId -> Event로 매핑한다. 키오스크 메뉴 표시(MenuService)와 결제 금액
// 계산(OrderService)이 "지금 이 맛에 할인이 살아있는가"를 같은 기준으로 판단하도록 공용으로 둔다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KioskFlavorDiscountService {

    private final EventBranchFlavorRepository eventBranchFlavorRepository;

    public Map<Long, Event> activeDiscountsByFlavor(Long branchId) {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, Event> result = new HashMap<>();
        for (EventBranchFlavor mapping : eventBranchFlavorRepository.findByBranch_BranchId(branchId)) {
            Event event = mapping.getEvent();
            if (isActiveFlavorDiscount(event, now)) {
                result.put(mapping.getFlavor().getFlavorId(), event);
            }
        }
        return result;
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

    private boolean isActiveFlavorDiscount(Event event, LocalDateTime now) {
        return event.getEventType() == EventType.FLAVOR_DISCOUNT
                && event.getStatus() == EventStatus.ACTIVE
                && !now.isBefore(event.getStartAt())
                && now.isBefore(event.getEndAt());
    }
}
