package com.kiosk.kiosk.order.service;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.common.Language;
import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.customer.CustomerRepository;
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
import com.kiosk.kiosk.order.dto.OrderCheckoutRequest;
import com.kiosk.kiosk.order.dto.OrderCheckoutResponse;
import com.kiosk.kiosk.order.dto.OrderItemRequest;
import java.util.ArrayList;
import java.util.List;
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

        private record ResolvedItem(Product product, OrderItemRequest request, int itemTotal) {
        }

        @Transactional
        public OrderCheckoutResponse checkout(OrderCheckoutRequest request) {
                Branch branch = branchRepository.findById(request.branchId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."));

                Customer customer = null;
                if (request.customerMobileNumber() != null && !request.customerMobileNumber().isBlank()) {
                        // 미등록 번호는 최하위 등급(FRIEND)·포인트 0으로 즉시 신규 가입시켜 이번 결제부터 적립 대상이 되게 함
                        customer = customerRepository.findByMobileNumber(request.customerMobileNumber())
                                        .orElseGet(() -> customerRepository.save(
                                                        Customer.builder().mobileNumber(request.customerMobileNumber())
                                                                        .build()));
                }

                List<ResolvedItem> resolved = new ArrayList<>();
                int amountBeforeDiscount = 0;
                for (OrderItemRequest itemRequest : request.items()) {
                        Product product = productRepository.findById(itemRequest.productId())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "상품을 찾을 수 없습니다."));
                        int itemTotal = Boolean.TRUE.equals(itemRequest.monthlyFlavorUpgrade())
                                        ? resolveMonthlyFlavorUpgradePrice(product, itemRequest)
                                        : product.getBasePrice();
                        if (itemRequest.containerType() == ContainerType.WAFFLE_CONE) {
                                itemTotal += 500;
                        }
                        amountBeforeDiscount += itemTotal;
                        resolved.add(new ResolvedItem(product, itemRequest, itemTotal));
                }

                int usedPoints = request.usedPoints() != null ? request.usedPoints() : 0;
                usedPoints = Math.max(0, Math.min(usedPoints, amountBeforeDiscount));
                usedPoints = customer != null ? Math.min(usedPoints, customer.getPointBalance()) : 0;
                int finalAmount = amountBeforeDiscount - usedPoints;

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
                                .discountAmount(usedPoints)
                                .finalAmount(finalAmount)
                                .build();
                order = orderRepository.save(order);
                int generatedWaitingNumber = (int) (order.getOrderId() % 1000);
                if (generatedWaitingNumber == 0)
                        generatedWaitingNumber = 1000;
                order.setWaitingNumber(generatedWaitingNumber);
                order = orderRepository.save(order);

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
                                usedPoints, finalAmount);
        }

        private int resolveMonthlyFlavorUpgradePrice(Product product, OrderItemRequest request) {
                boolean includesMonthlyFlavor = request.flavorIds() != null && request.flavorIds().stream()
                                .map(flavorId -> flavorRepository.findById(flavorId).orElse(null))
                                .filter(java.util.Objects::nonNull)
                                .map(Flavor::getFlavorName)
                                .anyMatch(name -> "쵸파의 코튼캔디 크런치".equals(name) || "우디의 후르츠 어드벤처".equals(name));
                if (!"더블주니어".equals(product.getProductName())
                                || request.flavorIds() == null
                                || request.flavorIds().size() != 2
                                || !includesMonthlyFlavor) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이달의 맛을 포함한 더블주니어만 사이즈업할 수 있습니다.");
                }
                Product singleRegular = productRepository.findAll().stream()
                                .filter(candidate -> "싱글레귤러".equals(candidate.getProductName()))
                                .findFirst()
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                                "싱글레귤러 상품을 찾을 수 없습니다."));
                return singleRegular.getBasePrice() + 500;
        }
}
