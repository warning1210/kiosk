package com.kiosk.kiosk.receipt;

import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderItem;
import com.kiosk.domain.order.OrderItemFlavor;
import com.kiosk.domain.order.OrderItemFlavorRepository;
import com.kiosk.domain.order.OrderItemRepository;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.payment.Payment;
import com.kiosk.domain.payment.PaymentRepository;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemFlavorRepository orderItemFlavorRepository;
    private final PaymentRepository paymentRepository;
    private final ReceiptPrintClient printClient;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy년MM월dd일");

    /**
     * 화면에 보여줄 영수증 데이터를 만든다. (주문 + 상품 + 맛 + 결제 정보를 한 번에)
     */
    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long orderId) {
        Order order = findOrder(orderId);
        Payment payment = paymentRepository.findByOrder_OrderId(orderId).orElse(null);

        List<ReceiptItemResponse> items = new ArrayList<>();
        for (OrderItem orderItem : orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(orderId)) {
            items.add(new ReceiptItemResponse(
                    orderItem.getProductNameSnapshot(),
                    orderItem.getQuantity(),
                    orderItem.getItemTotal(),
                    orderItem.getContainerType().name(),
                    orderItem.getSpoonCount() == null ? 0 : orderItem.getSpoonCount().intValue(),
                    orderItem.getDryIceMinutes() == null ? null : orderItem.getDryIceMinutes().intValue(),
                    groupFlavors(orderItem.getOrderItemId())
            ));
        }

        return new ReceiptResponse(
                order.getBranch().getBranchName(),
                order.getOrderNumber(),
                order.getWaitingNumber(),
                order.getOrderType().name(),
                payment != null ? payment.getPaidAt() : null,
                items,
                order.getAmountBeforeDiscount(),
                order.getDiscountAmount(),
                order.getFinalAmount(),
                order.getEarnedPoints(),
                payment != null ? payment.getPaymentMethod().name() : null,
                payment != null ? payment.getApprovalNumber() : null
        );
    }

    /**
     * 강사님 프린터 서비스로 영수증 출력을 요청한다.
     * @return 실제로 출력됐으면 true, 프린터가 오프라인이면 false (주문은 그대로 유효)
     */
    @Transactional(readOnly = true)
    public boolean printReceipt(Long orderId) {
        Order order = findOrder(orderId);
        Payment payment = paymentRepository.findByOrder_OrderId(orderId).orElse(null);

        LocalDateTime when = (payment != null && payment.getPaidAt() != null)
                ? payment.getPaidAt()
                : LocalDateTime.now();

        PrinterReceiptRequest request = new PrinterReceiptRequest(
                order.getOrderNumber(),
                buildItemSummary(orderId),
                formatWon(order.getFinalAmount()),
                when.format(DATE_FORMAT)
        );
        return printClient.print(request);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    // 같은 맛을 여러 번 골랐으면(예: 초코 2스쿱) 화면에서 "초코 x2" 로 묶어 보여준다.
    private List<ReceiptFlavorResponse> groupFlavors(Long orderItemId) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (OrderItemFlavor flavor : orderItemFlavorRepository.findByOrderItem_OrderItemIdOrderBySelectionOrderAsc(orderItemId)) {
            counts.merge(flavor.getFlavorNameSnapshot(), 1, Integer::sum);
        }
        List<ReceiptFlavorResponse> result = new ArrayList<>();
        counts.forEach((name, count) -> result.add(new ReceiptFlavorResponse(name, count)));
        return result;
    }

    // 프린터에는 한 줄로 짧게: 첫 상품명 + "외 N건" (예: "파인트 외 2건")
    private String buildItemSummary(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrder_OrderIdOrderByOrderItemIdAsc(orderId);
        if (items.isEmpty()) {
            return "주문 상품";
        }
        String firstName = items.get(0).getProductNameSnapshot();
        if (items.size() == 1) {
            return firstName;
        }
        return firstName + " 외 " + (items.size() - 1) + "건";
    }

    // 13700 -> "13,700원"
    private String formatWon(int amount) {
        return NumberFormat.getInstance(Locale.KOREA).format(amount) + "원";
    }
}
