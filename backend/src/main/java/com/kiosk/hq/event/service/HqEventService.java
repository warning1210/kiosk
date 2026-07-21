package com.kiosk.hq.event.service;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.event.BenefitType;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventRepository;
import com.kiosk.domain.event.EventStatus;
import com.kiosk.domain.event.EventType;
import com.kiosk.hq.event.dto.HqEventCreateRequest;
import com.kiosk.hq.event.dto.HqEventResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 본점 이벤트 생성/조회. 생성 즉시 ACTIVE로 만들고(별도 승인/예약 단계 없음),
// 지점 쪽 노출 여부는 branch/notice에서 start_at~end_at 기간을 조회 시점에 걸러서 판단한다.
@Service
@RequiredArgsConstructor
@Transactional
public class HqEventService {

    private final EventRepository eventRepository;

    public HqEventResponse create(HqEventCreateRequest request, Admin creator) {
        if (request.startAt() == null || request.endAt() == null || !request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("종료일시는 시작일시보다 늦어야 합니다.");
        }

        EventType eventType = parseEnum(EventType.class, request.eventType(), "이벤트 유형");

        // COUPON형은 실제 할인값이 개별 쿠폰(HqCouponService)에 있으므로 이벤트 자체엔 할인값이 필요 없다.
        // FLAVOR_DISCOUNT형은 이벤트가 직접 할인값을 갖고, 어느 맛에 붙일지만 지점이 나중에 고른다.
        BenefitType benefitType;
        BigDecimal discountRate = null;
        Integer discountAmount = null;
        if (eventType == EventType.COUPON) {
            benefitType = BenefitType.COUPON;
        } else {
            benefitType = parseEnum(BenefitType.class, request.benefitType(), "혜택 유형");
            if (benefitType == BenefitType.DISCOUNT_RATE) {
                if (request.discountRate() == null || request.discountRate().compareTo(BigDecimal.ZERO) <= 0
                        || request.discountRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("할인율은 0보다 크고 100 이하여야 합니다.");
                }
                discountRate = request.discountRate();
            } else if (benefitType == BenefitType.DISCOUNT_AMOUNT) {
                if (request.discountAmount() == null || request.discountAmount() <= 0) {
                    throw new IllegalArgumentException("할인 금액을 올바르게 입력해주세요.");
                }
                discountAmount = request.discountAmount();
            } else {
                throw new IllegalArgumentException("상품(맛) 할인 이벤트는 할인율 또는 할인 금액 중 하나여야 합니다.");
            }
        }

        Event event = Event.builder()
                .eventName(request.eventName())
                .eventType(eventType)
                .benefitType(benefitType)
                .description(request.description())
                .imageUrl(request.imageUrl())
                .discountRate(discountRate)
                .discountAmount(discountAmount)
                .startAt(request.startAt())
                .endAt(request.endAt())
                .status(EventStatus.ACTIVE)
                .creatorAdmin(creator)
                .build();

        return HqEventResponse.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<HqEventResponse> list() {
        return eventRepository.findAll().stream()
                .sorted(Comparator.comparing(Event::getCreatedAt).reversed())
                .map(HqEventResponse::from)
                .toList();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String fieldLabel) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("올바르지 않은 " + fieldLabel + "입니다: " + value);
        }
    }
}
