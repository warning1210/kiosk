package com.kiosk.branch.dashboard.service;

import com.kiosk.branch.dashboard.dto.BranchDashboardSummaryDto;
import com.kiosk.domain.order.Order;
import com.kiosk.domain.order.OrderRepository;
import com.kiosk.domain.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchDashboardService {

    private final OrderRepository orderRepository;

    public BranchDashboardSummaryDto getDashboardSummary(Long branchId) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 신규 주문 (PAID)
        long newOrderCount = orderRepository.countByBranchIdAndOrderStatusAndCreatedAtBetween(branchId, OrderStatus.PAID, startOfDay, endOfDay);
        
        // 처리 중 (MAKING)
        long processingOrderCount = orderRepository.countByBranchIdAndOrderStatusAndCreatedAtBetween(branchId, OrderStatus.MAKING, startOfDay, endOfDay);
        
        // 오늘 완료 (COMPLETED)
        long completedOrderCount = orderRepository.countByBranchIdAndOrderStatusAndCreatedAtBetween(branchId, OrderStatus.COMPLETED, startOfDay, endOfDay);

        // 오늘 매출 (취소되지 않은 PAID, MAKING, COMPLETED 주문들의 finalAmount 총합)
        List<Order> validOrders = orderRepository.findByBranchIdAndOrderStatusInAndCreatedAtBetween(
                branchId, 
                List.of(OrderStatus.PAID, OrderStatus.MAKING, OrderStatus.COMPLETED), 
                startOfDay, 
                endOfDay
        );
        
        long todaySalesAmount = validOrders.stream()
                .mapToLong(Order::getFinalAmount)
                .sum();

        return BranchDashboardSummaryDto.builder()
                .newOrderCount(newOrderCount)
                .processingOrderCount(processingOrderCount)
                .completedOrderCount(completedOrderCount)
                .todaySalesAmount(todaySalesAmount)
                .build();
    }
}
