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

        switch (eventType) {
            case MONTHLY_FLAVOR -> {
                // 할인이 없는 대신 사이즈업이 곧 그 혜택이다 - 아무 혜택 없이 "이달의 맛" 표시만 하는 건
                // 이벤트라고 할 수 없으므로, 이 맛을 고르면 반드시 사이즈업이 같이 걸리게 한다.
                builder.flavor(requireFlavor(request));
                applySizeUp(builder, request);
            }
            case HQ_FLAVOR_DISCOUNT -> {
                // 본점이 맛과 할인값을 둘 다 직접 정한다 - 지점이 고를 게 없다는 점만 FLAVOR_DISCOUNT와 다르다.
                builder.flavor(requireFlavor(request));
                applyDiscount(builder, request);
            }
            case FLAVOR_DISCOUNT -> applyDiscount(builder, request);
        }

        return HqEventResponse.from(eventRepository.save(builder.build()));
    }

    private Flavor requireFlavor(HqEventCreateRequest request) {
        if (request.flavorId() == null) {
            throw new IllegalArgumentException("맛을 선택해주세요.");
        }
        return flavorRepository.findById(request.flavorId())
                .orElseThrow(() -> new IllegalArgumentException("맛을 찾을 수 없습니다."));
    }

    private void applyDiscount(Event.EventBuilder builder, HqEventCreateRequest request) {
        BenefitType benefitType = parseEnum(BenefitType.class, request.benefitType(), "혜택 유형");
        builder.benefitType(benefitType);
        if (benefitType == BenefitType.DISCOUNT_RATE) {
            if (request.discountRate() == null || request.discountRate().compareTo(BigDecimal.ZERO) <= 0
                    || request.discountRate().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("할인율은 0보다 크고 100 이하여야 합니다.");
            }
            builder.discountRate(request.discountRate());
        } else {
            if (request.discountAmount() == null || request.discountAmount() <= 0) {
                throw new IllegalArgumentException("할인 금액을 올바르게 입력해주세요.");
            }
            builder.discountAmount(request.discountAmount());
        }
    }

    // "사이즈업 후 상품"은 본점이 직접 고른다 - 인접한 다음 사이즈로 자동 계산하면 "이달의 더블주니어"처럼
    // 중간 단계(싱글킹)를 건너뛰는 프로모션을 만들 수 없기 때문이다. 같은 카테고리 안이라면 임의의 상품을 골라도 된다.
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
                .orElseThrow(() -> new IllegalArgumentException("사이즈업 적용 상품을 찾을 수 없습니다."));
        Product toProduct = productRepository.findById(request.sizeUpToProductId())
                .orElseThrow(() -> new IllegalArgumentException("사이즈업 후 상품을 찾을 수 없습니다."));

        // 파인트 이상 대용량(스쿱 3개 이상) 상품은 사이즈업 대상이 아니다 - 싱글레귤러/싱글킹/더블주니어/더블레귤러까지만.
        if (isTooLargeForSizeUp(fromProduct) || isTooLargeForSizeUp(toProduct)) {
            throw new IllegalArgumentException("사이즈업은 싱글레귤러/싱글킹/더블주니어/더블레귤러 사이에서만 가능합니다.");
        }

        // 실제 정가 차이 이상을 추가금액으로 받으면 사이즈업이 아니라 그냥 큰 사이즈를 따로 사는 것과 같거나
        // 더 비싸진다 - 그러면 "사이즈업 할인" 자체가 의미 없으므로 반드시 정가 차이보다 싸야 한다.
        int priceGap = toProduct.getBasePrice() - fromProduct.getBasePrice();
        if (priceGap <= 0) {
            throw new IllegalArgumentException("사이즈업 후 상품은 적용 상품보다 정가가 더 비싸야 합니다.");
        }
        if (request.additionalPayment() >= priceGap) {
            throw new IllegalArgumentException(
                    "추가 금액은 정가 차이(" + priceGap + "원)보다 작아야 합니다. " + toProduct.getProductName()
                            + " 정가가 " + fromProduct.getProductName() + "보다 " + priceGap + "원 더 비쌉니다.");
        }

        builder.sizeUpFromProduct(fromProduct)
                .sizeUpToProduct(toProduct)
                .additionalPayment(request.additionalPayment());
    }

    private boolean isTooLargeForSizeUp(Product product) {
        return product.getSelectableFlavorCount() == null || product.getSelectableFlavorCount() > 2;
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
