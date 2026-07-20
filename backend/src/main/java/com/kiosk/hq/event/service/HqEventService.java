package com.kiosk.hq.event.service;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.event.BenefitType;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventRepository;
import com.kiosk.domain.event.EventStatus;
import com.kiosk.domain.event.EventType;
import com.kiosk.hq.event.dto.HqEventCreateRequest;
import com.kiosk.hq.event.dto.HqEventResponse;
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
        BenefitType benefitType = request.benefitType() == null || request.benefitType().isBlank()
                ? BenefitType.NONE
                : parseEnum(BenefitType.class, request.benefitType(), "혜택 유형");

        Event event = Event.builder()
                .eventName(request.eventName())
                .eventType(eventType)
                .benefitType(benefitType)
                .description(request.description())
                .imageUrl(request.imageUrl())
                .discountRate(request.discountRate())
                .discountAmount(request.discountAmount())
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
