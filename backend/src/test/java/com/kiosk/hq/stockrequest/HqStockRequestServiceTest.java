package com.kiosk.hq.stockrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class HqStockRequestServiceTest {

    @Mock
    private StockRequestRepository stockRequestRepository;
    @Mock
    private StockRequestItemRepository stockRequestItemRepository;

    @InjectMocks
    private HqStockRequestService service;

    @Test
    void approveUsesAllRequestedQuantities() {
        Admin hq = hqAdmin();
        Branch branch = branch();
        Admin requester = branchManager(branch);
        StockRequest stockRequest = stockRequest(branch, requester, StockRequestStatus.PENDING);
        StockRequestItem first = item(stockRequest, flavor(100L, "바닐라"), 2);
        StockRequestItem second = item(stockRequest, flavor(101L, "초콜릿"), 5);

        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));
        when(stockRequestItemRepository.findByStockRequest_StockRequestId(50L))
                .thenReturn(List.of(first, second));

        StockRequestResponse response = service.approve(hq, 50L);

        assertEquals(StockRequestStatus.PREPARING, response.requestStatus());
        assertEquals(2, first.getApprovedQuantity());
        assertEquals(5, second.getApprovedQuantity());
        assertSame(hq, stockRequest.getProcessedAdmin());
        assertNotNull(stockRequest.getProcessedAt());
    }

    @Test
    void listUsesExplicitQueryAndNormalizesKeyword() {
        Admin hq = hqAdmin();
        Branch branch = branch();
        Admin requester = branchManager(branch);
        LocalDateTime from = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 7, 31, 23, 59);
        Pageable pageable = PageRequest.of(0, 20);
        StockRequest stockRequest = stockRequest(branch, requester, StockRequestStatus.PENDING);
        Page<StockRequest> page = new PageImpl<>(List.of(stockRequest), pageable, 1);

        when(stockRequestRepository.searchForHq(
                StockRequestStatus.PENDING, 1L, from, to, "%강남%", pageable)).thenReturn(page);
        when(stockRequestItemRepository.findByStockRequestIdIn(List.of(50L))).thenReturn(List.of());

        Page<StockRequestResponse> result = service.list(
                hq, StockRequestStatus.PENDING, 1L, from, to, "  강남  ", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(50L, result.getContent().getFirst().stockRequestId());
        verify(stockRequestRepository).searchForHq(
                StockRequestStatus.PENDING, 1L, from, to, "%강남%", pageable);
    }

    private StockRequest stockRequest(Branch branch, Admin requester, StockRequestStatus status) {
        return StockRequest.builder()
                .stockRequestId(50L)
                .requestNumber("REQ-20260713-50")
                .branch(branch)
                .requesterAdmin(requester)
                .requestStatus(status)
                .urgency(Urgency.NORMAL)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    private StockRequestItem item(StockRequest request, Flavor flavor, int quantity) {
        return StockRequestItem.builder()
                .stockRequest(request)
                .flavor(flavor)
                .requestedQuantity(quantity)
                .build();
    }

    private Branch branch() {
        return Branch.builder()
                .branchId(1L)
                .branchName("강남점")
                .address("서울")
                .build();
    }

    private Admin branchManager(Branch branch) {
        return Admin.builder()
                .adminId(10L)
                .branch(branch)
                .loginId("manager")
                .passwordHash("pw")
                .name("점장")
                .role(AdminRole.BRANCH_MANAGER)
                .build();
    }

    private Admin hqAdmin() {
        return Admin.builder()
                .adminId(20L)
                .loginId("hq")
                .passwordHash("pw")
                .name("본점 관리자")
                .role(AdminRole.HQ_ADMIN)
                .build();
    }

    private Flavor flavor(long id, String name) {
        return Flavor.builder()
                .flavorId(id)
                .flavorName(name)
                .build();
    }
}
