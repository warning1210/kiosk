package com.example.kiosksim.service;

import com.example.kiosksim.domain.KioskOrder;
import com.example.kiosksim.domain.OrderItem;
import com.example.kiosksim.domain.OrderItemFlavor;
import com.example.kiosksim.domain.Payment;
import com.example.kiosksim.dto.CartFlavorRequest;
import com.example.kiosksim.dto.CartItemRequest;
import com.example.kiosksim.dto.DiscountRequest;
import com.example.kiosksim.dto.FlavorResponse;
import com.example.kiosksim.dto.OrderDraftRequest;
import com.example.kiosksim.dto.OrderItemFlavorResponse;
import com.example.kiosksim.dto.OrderItemResponse;
import com.example.kiosksim.dto.OrderResponse;
import com.example.kiosksim.dto.PaymentResponse;
import com.example.kiosksim.dto.ProductResponse;
import com.example.kiosksim.dto.TableSnapshotResponse;
import com.example.kiosksim.repository.KioskOrderRepository;
import com.example.kiosksim.repository.OrderItemFlavorRepository;
import com.example.kiosksim.repository.OrderItemRepository;
import com.example.kiosksim.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final CatalogService catalogService;
    private final KioskOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemFlavorRepository flavorRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(CatalogService catalogService, KioskOrderRepository orderRepository,
                        OrderItemRepository orderItemRepository, OrderItemFlavorRepository flavorRepository,
                        PaymentRepository paymentRepository) {
        this.catalogService = catalogService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.flavorRepository = flavorRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public OrderResponse checkout(OrderDraftRequest request) {
        validateOrderType(request.orderType());

        int originalAmount = 0;
        for (CartItemRequest item : request.cartItems()) {
            ProductResponse product = catalogService.requireProduct(item.productId());
            validateItem(request.orderType(), product, item);
            originalAmount += product.basePrice() * item.quantity();
        }

        DiscountRequest discount = request.discount() == null
                ? new DiscountRequest(null, 0, 0)
                : request.discount();
        int discountAmount = discount.safeCouponDiscountAmount() + discount.safeUsedPoint();
        int finalAmount = Math.max(originalAmount - discountAmount, 0);

        LocalDateTime now = LocalDateTime.now();
        KioskOrder order = orderRepository.save(new KioskOrder(
                request.branchId(),
                request.kioskId(),
                "ORD-" + now.toLocalDate() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                (int) ((orderRepository.count() % 900) + 100),
                request.orderType(),
                "PAID",
                discount.safeUsedPoint(),
                originalAmount,
                discountAmount,
                finalAmount,
                now
        ));

        for (CartItemRequest item : request.cartItems()) {
            ProductResponse product = catalogService.requireProduct(item.productId());
            OrderItem savedItem = orderItemRepository.save(new OrderItem(
                    order.getId(),
                    product.id(),
                    product.name(),
                    item.quantity(),
                    product.basePrice(),
                    product.basePrice() * item.quantity(),
                    item.containerType(),
                    item.spoonCount(),
                    item.dryIceMinutes()
            ));

            List<CartFlavorRequest> flavors = item.flavors() == null ? List.of() : item.flavors();
            for (CartFlavorRequest flavor : flavors) {
                FlavorResponse catalogFlavor = catalogService.requireFlavor(flavor.flavorId());
                flavorRepository.save(new OrderItemFlavor(
                        savedItem.getId(),
                        catalogFlavor.id(),
                        catalogFlavor.name(),
                        flavor.selectOrder(),
                        1
                ));
            }
        }

        paymentRepository.save(new Payment(
                order.getId(),
                "QR",
                "PAID",
                "QR-" + UUID.randomUUID(),
                now.plusMinutes(3),
                finalAmount,
                now
        ));

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(KioskOrder::getOrderedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        KioskOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + id));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public TableSnapshotResponse tableSnapshot() {
        List<OrderResponse> orders = findAll();
        return new TableSnapshotResponse(
                orders,
                orderRepository.count(),
                orderItemRepository.count(),
                flavorRepository.count(),
                paymentRepository.count()
        );
    }

    private void validateOrderType(String orderType) {
        if (!"DINE_IN".equals(orderType) && !"TAKEOUT".equals(orderType)) {
            throw new IllegalArgumentException("orderType은 DINE_IN 또는 TAKEOUT이어야 합니다.");
        }
    }

    private void validateItem(String orderType, ProductResponse product, CartItemRequest item) {
        if (item.quantity() == null || item.quantity() < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }

        if ("TAKEOUT".equals(orderType) && "CONE".equals(item.containerType())) {
            throw new IllegalArgumentException("포장 주문은 콘을 선택할 수 없습니다.");
        }

        List<CartFlavorRequest> flavors = item.flavors() == null ? List.of() : item.flavors();
        if (product.requiresFlavor() && flavors.size() != product.selectableFlavorCount()) {
            throw new IllegalArgumentException(product.name() + "는 맛을 "
                    + product.selectableFlavorCount() + "개 선택해야 합니다.");
        }

        if (!product.requiresFlavor() && !flavors.isEmpty()) {
            throw new IllegalArgumentException(product.name() + "는 맛 선택이 없는 상품입니다.");
        }

        for (CartFlavorRequest flavor : flavors) {
            catalogService.requireFlavor(flavor.flavorId());
        }
    }

    private OrderResponse toResponse(KioskOrder order) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(this::toItemResponse)
                .toList();

        PaymentResponse payment = paymentRepository.findByOrderId(order.getId())
                .map(this::toPaymentResponse)
                .orElse(null);

        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getWaitingNo(),
                order.getOrderType(),
                order.getStatus(),
                order.getOriginalAmount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getOrderedAt(),
                items,
                payment
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        List<OrderItemFlavorResponse> flavors = flavorRepository.findByOrderItemId(item.getId()).stream()
                .map(flavor -> new OrderItemFlavorResponse(
                        flavor.getId(),
                        flavor.getFlavorId(),
                        flavor.getFlavorNameSnapshot(),
                        flavor.getSelectOrder(),
                        flavor.getQuantity()
                ))
                .toList();

        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal(),
                item.getContainerType(),
                item.getSpoonCount(),
                item.getDryIceMinutes(),
                flavors
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getQrToken(),
                payment.getQrExpiresAt(),
                payment.getPaidAmount(),
                payment.getPaidAt()
        );
    }
}
