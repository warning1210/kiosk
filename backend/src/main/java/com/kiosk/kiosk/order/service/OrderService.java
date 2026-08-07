package com.kiosk.kiosk.order.service;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.common.Language;
import com.kiosk.domain.coupon.Coupon;
import com.kiosk.domain.coupon.CouponRepository;
import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.customer.CustomerRepository;
import com.kiosk.domain.event.Event;
import com.kiosk.domain.event.EventType;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.order.ContainerType;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderItem;
import com.kiosk.domain.order.OrderItemFlavor;
import com.kiosk.domain.order.OrderItemFlavorRepository;
import com.kiosk.domain.order.OrderItemRepository;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import com.kiosk.domain.product.Product;
import com.kiosk.domain.product.ProductRepository;
import com.kiosk.global.security.MobileNumberCrypto;
import com.kiosk.kiosk.coupon.service.CouponValidationService;
import com.kiosk.kiosk.event.service.KioskFlavorDiscountService;
import com.kiosk.kiosk.order.dto.OrderCheckoutRequest;
import com.kiosk.kiosk.order.dto.OrderCheckoutResponse;
import com.kiosk.kiosk.order.dto.OrderItemRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final OrderItemFlavorRepository orderItemFlavorRepository;
        private final BranchRepository branchRepository;
        private final ProductRepository productRepository;
        private final FlavorRepository flavorRepository;
        private final CustomerRepository customerRepository;
        private final CouponRepository couponRepository;
        private final CouponValidationService couponValidationService;
        private final KioskFlavorDiscountService kioskFlavorDiscountService;

        private record ResolvedItem(Product product, OrderItemRequest request, int itemTotal) {
        }

        @Transactional
        public OrderCheckoutResponse checkout(OrderCheckoutRequest request) {
                if (request.items() == null || request.items().isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다.");
                }

                Branch branch = branchRepository.findById(request.branchId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."));

                Customer customer = null;
                if (request.customerMobileNumber() != null && !request.customerMobileNumber().isBlank()) {
                        String rawMobileNumber = request.customerMobileNumber();
                        String mobileNumberHash = MobileNumberCrypto.hash(rawMobileNumber);
                        // 미등록 번호는 최하위 등급(FRIEND)·포인트 0으로 즉시 신규 가입시켜 이번 결제부터 적립 대상이 되게 함
                        customer = customerRepository.findByMobileNumberHash(mobileNumberHash)
                                        .orElseGet(() -> customerRepository.save(
                                                        Customer.builder()
                                                                        .mobileNumberHash(mobileNumberHash)
                                                                        .mobileNumberMasked(MobileNumberCrypto.mask(rawMobileNumber))
                                                                        .build()));
                }

                Map<Long, Event> activeFlavorDiscounts = kioskFlavorDiscountService.activeDiscountsByFlavor(branch.getBranchId());

                List<ResolvedItem> resolved = new ArrayList<>();
                int amountBeforeDiscount = 0;
                for (OrderItemRequest itemRequest : request.items()) {
                        Product product = productRepository.findById(itemRequest.productId())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "상품을 찾을 수 없습니다."));
                        int itemTotal = resolveItemBasePrice(product, itemRequest, activeFlavorDiscounts);
                        if (itemRequest.containerType() == ContainerType.WAFFLE_CONE) {
                                itemTotal += 500;
                        }
                        // 지점이 이 상품에 담긴 맛 중 하나를 "상품(맛) 할인" 이벤트 대상으로 골라뒀다면 반영한다
                        // (여러 맛에 할인이 겹쳐도 중복 적용 없이 가장 큰 할인 하나만 뺀다)
                        int flavorDiscount = kioskFlavorDiscountService.resolveDiscount(activeFlavorDiscounts, itemRequest.flavorIds(), itemTotal);
                        itemTotal = Math.max(0, itemTotal - flavorDiscount);
                        amountBeforeDiscount += itemTotal;
                        resolved.add(new ResolvedItem(product, itemRequest, itemTotal));
                }

                int usedPoints = request.usedPoints() != null ? request.usedPoints() : 0;
                usedPoints = Math.max(0, Math.min(usedPoints, amountBeforeDiscount));
                usedPoints = customer != null ? Math.min(usedPoints, customer.getPointBalance()) : 0;

                Coupon coupon = couponValidationService.validate(request.couponCode());
                int couponDiscount = coupon != null
                                ? Math.min(coupon.calculateDiscount(amountBeforeDiscount), Math.max(0, amountBeforeDiscount - usedPoints))
                                : 0;
                int discountAmount = usedPoints + couponDiscount;
                int finalAmount = amountBeforeDiscount - discountAmount;

                Order order = Order.builder()
                                .branch(branch)
                                .customer(customer)
                                .orderNumber("ORD" + System.currentTimeMillis() + "-"
                                                + UUID.randomUUID().toString().substring(0, 6))
                                .orderType(request.orderType())
                                .orderStatus(OrderStatus.PENDING_PAYMENT)
                                .language(request.language() != null ? request.language() : Language.ko)
                                .usedPoints(usedPoints)
                                .earnedPoints(0)
                                .amountBeforeDiscount(amountBeforeDiscount)
                                .discountAmount(discountAmount)
                                .finalAmount(finalAmount)
                                .build();
                order = orderRepository.save(order);
                int generatedWaitingNumber = (int) (order.getOrderId() % 1000);
                if (generatedWaitingNumber == 0)
                        generatedWaitingNumber = 1000;
                order.setWaitingNumber(generatedWaitingNumber);
                order = orderRepository.save(order);

                // 쿠폰은 체크아웃 시점엔 이 주문에 "연결"만 해두고, 실제 소진(USED) 처리는 결제가 완료됐을 때
                // PaymentService.markPaidAndComplete()에서 한다 - 포인트 차감과 동일한 타이밍 정책.
                if (coupon != null) {
                        coupon.setUsedOrder(order);
                        couponRepository.save(coupon);
                }

                for (ResolvedItem item : resolved) {
                        OrderItem orderItem = OrderItem.builder()
                                        .order(order)
                                        .product(item.product())
                                        .productNameSnapshot(item.product().getProductName())
                                        .unitPriceSnapshot(item.itemTotal())
                                        .quantity(1)
                                        .itemTotal(item.itemTotal())
                                        .containerType(item.request().containerType() != null
                                                        ? item.request().containerType()
                                                        : ContainerType.NONE)
                                        .spoonCount(item.request().spoonCount() != null
                                                        ? item.request().spoonCount().byteValue()
                                                        : (byte) 0)
                                        .dryIceMinutes(item.request().dryIceMinutes() != null
                                                        ? item.request().dryIceMinutes().byteValue()
                                                        : null)
                                        .build();
                        orderItem = orderItemRepository.save(orderItem);

                        List<Long> flavorIds = item.request().flavorIds() != null ? item.request().flavorIds()
                                        : List.of();
                        int selectionOrder = 1;
                        for (Long flavorId : flavorIds) {
                                Flavor flavor = flavorRepository.findById(flavorId)
                                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                "맛을 찾을 수 없습니다."));
                                OrderItemFlavor orderItemFlavor = OrderItemFlavor.builder()
                                                .orderItem(orderItem)
                                                .flavor(flavor)
                                                .flavorNameSnapshot(flavor.getFlavorName())
                                                .selectionOrder((byte) (selectionOrder++))
                                                .quantity((byte) 1)
                                                .build();
                                orderItemFlavorRepository.save(orderItemFlavor);
                        }
                }

                return new OrderCheckoutResponse(order.getOrderId(), order.getOrderNumber(), amountBeforeDiscount,
                                discountAmount, finalAmount);
        }

        // 이 상품이 진행 중인 MONTHLY_FLAVOR(사이즈업) 이벤트의 "후" 상품이고, 실제로 그 이벤트가 지정한 맛을
        // 담았다면 사이즈업 가격을 적용한다 - 싱글레귤러에서 업그레이드해왔든, 처음부터 이 상품을 고르고 그
        // 맛을 담았든 상관없이 같은 규칙이다 (사이즈업은 "이 맛을 고르면"이 조건이지 어떻게 도달했는지가 아님).
        private int resolveItemBasePrice(Product product, OrderItemRequest request, Map<Long, Event> activeFlavorDiscounts) {
                var configuredSizeUp = kioskFlavorDiscountService.activeSizeUpEventTo(product.getProductId())
                                .filter(event -> event.getFlavor() != null
                                                && request.flavorIds() != null
                                                && request.flavorIds().contains(event.getFlavor().getFlavorId()))
                                .map(event -> event.getSizeUpFromProduct().getBasePrice() + event.getAdditionalPayment());
                if (configuredSizeUp.isPresent()) {
                        return configuredSizeUp.get();
                }

                // 기존 DB의 이달의 맛 이벤트는 사이즈업 상품 컬럼이 비어 있을 수 있다. 그런 데이터도
                // 더블주니어에 이달의 맛이 실제 포함됐을 때만 싱글레귤러 가격 + 500원으로 서버에서 확정한다.
                boolean containsLegacyMonthlyFlavor = request.flavorIds() != null
                                && request.flavorIds().stream()
                                                .map(activeFlavorDiscounts::get)
                                                .anyMatch(event -> event != null
                                                                && event.getEventType() == EventType.MONTHLY_FLAVOR);
                if (containsLegacyMonthlyFlavor && "더블주니어".equals(product.getProductName())) {
                        return productRepository.findAll().stream()
                                        .filter(candidate -> "싱글레귤러".equals(candidate.getProductName()))
                                        .findFirst()
                                        .map(candidate -> candidate.getBasePrice() + 500)
                                        .orElse(product.getBasePrice());
                }
                return product.getBasePrice();
        }
}
