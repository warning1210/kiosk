package com.kiosk.hq.event.service;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.event.BenefitType;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventRepository;
import com.kiosk.domain.event.EventStatus;
import com.kiosk.domain.event.EventType;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.product.Product;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.hq.event.dto.HqEventCreateRequest;
import com.kiosk.hq.event.dto.HqEventResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final FlavorRepository flavorRepository;
    private final ProductRepository productRepository;

    public HqEventResponse create(HqEventCreateRequest request, Admin creator) {
        if (request.startAt() == null || request.endAt() == null || !request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException("종료일시는 시작일시보다 늦어야 합니다.");
        }

        EventType eventType = parseEnum(EventType.class, request.eventType(), "이벤트 유형");

        Event.EventBuilder builder = Event.builder()
                .eventName(request.eventName())
                .eventType(eventType)
                .description(request.description())
                .imageUrl(request.imageUrl())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .status(EventStatus.ACTIVE)
                .creatorAdmin(creator);

        if (eventType == EventType.SIZE_UP) {
            applySizeUp(builder, request);
        } else {
            // MONTHLY_FLAVOR(본점 직접 지정 할인 맛) / FLAVOR_DISCOUNT(지점이 나중에 맛을 고름) 공통 -
            // 할인값(율/금액)은 이벤트가 직접 갖는다.
            BenefitType benefitType = parseEnum(BenefitType.class, request.benefitType(), "혜택 유형");
            if (benefitType == BenefitType.DISCOUNT_RATE) {
                if (request.discountRate() == null || request.discountRate().compareTo(BigDecimal.ZERO) <= 0
                        || request.discountRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("할인율은 0보다 크고 100 이하여야 합니다.");
                }
                builder.benefitType(benefitType).discountRate(request.discountRate());
            } else if (benefitType == BenefitType.DISCOUNT_AMOUNT) {
                if (request.discountAmount() == null || request.discountAmount() <= 0) {
                    throw new IllegalArgumentException("할인 금액을 올바르게 입력해주세요.");
                }
                builder.benefitType(benefitType).discountAmount(request.discountAmount());
            } else {
                throw new IllegalArgumentException("상품(맛) 할인 이벤트는 할인율 또는 할인 금액 중 하나여야 합니다.");
            }

            if (eventType == EventType.MONTHLY_FLAVOR) {
                if (request.flavorId() == null) {
                    throw new IllegalArgumentException("할인을 붙일 맛을 선택해주세요.");
                }
                Flavor flavor = flavorRepository.findById(request.flavorId())
                        .orElseThrow(() -> new IllegalArgumentException("맛을 찾을 수 없습니다."));
                builder.flavor(flavor);
            }
        }

        return HqEventResponse.from(eventRepository.save(builder.build()));
    }

    private void applySizeUp(Event.EventBuilder builder, HqEventCreateRequest request) {
        if (request.sizeUpFromProductId() == null || request.sizeUpToProductId() == null) {
            throw new IllegalArgumentException("사이즈업 전/후 상품을 모두 선택해주세요.");
        }
        if (request.sizeUpFromProductId().equals(request.sizeUpToProductId())) {
            throw new IllegalArgumentException("사이즈업 전/후 상품은 서로 달라야 합니다.");
        }
        if (request.additionalPayment() == null || request.additionalPayment() < 0) {
            throw new IllegalArgumentException("추가 금액을 올바르게 입력해주세요.");
        }
        Product fromProduct = productRepository.findById(request.sizeUpFromProductId())
                .orElseThrow(() -> new IllegalArgumentException("사이즈업 전 상품을 찾을 수 없습니다."));
        Product toProduct = productRepository.findById(request.sizeUpToProductId())
                .orElseThrow(() -> new IllegalArgumentException("사이즈업 후 상품을 찾을 수 없습니다."));
        builder.benefitType(BenefitType.SIZE_UP)
                .sizeUpFromProduct(fromProduct)
                .sizeUpToProduct(toProduct)
                .additionalPayment(request.additionalPayment());
    }

    @Transactional(readOnly = true)
    public List<HqEventResponse> list() {
        return eventRepository.findAll().stream()
                .sorted(Comparator.comparing(Event::getCreatedAt).reversed())
                .map(HqEventResponse::from)
                .toList();
    }

    // 원래 종료일(endAt) 전에 본점이 즉시 종료시킨다 - 예정된 기간을 다 채우고 끝난 것(ENDED)과 구분하기 위해
    // CANCELLED로 표시하고, endAt도 실제로 끊긴 시점으로 당겨서 화면에 그 시점이 그대로 보이게 한다.
    // FLAVOR_DISCOUNT는 status만 바뀌어도 KioskFlavorDiscountService가 곧바로 할인 적용을 멈춘다
    // (ACTIVE 여부를 같이 검사하므로).
    public HqEventResponse end(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        if (event.getStatus() == EventStatus.ENDED || event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("이미 종료된 이벤트입니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        event.setStatus(EventStatus.CANCELLED);
        // 아직 시작 전(SCHEDULED)에 취소된 거라면 "얼마나 진행되다 끊겼는지"가 없으니 원래 기간을 그대로 둔다
        if (now.isAfter(event.getStartAt())) {
            event.setEndAt(now);
        }
        return HqEventResponse.from(eventRepository.save(event));
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String fieldLabel) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception e) {
            throw new IllegalArgumentException("올바르지 않은 " + fieldLabel + "입니다: " + value);
        }
    }
}
