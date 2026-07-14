package com.kiosk.hq.stockrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.kiosk.hq.stockrequest.dto.RejectRequest;
import com.kiosk.hq.stockrequest.dto.ShipRequest;
import com.kiosk.stockrequest.dto.StockRequestResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * 본사 재고신청 서비스의 승인·반려·배송 규칙을 검증하는 단위 테스트다.
 * Repository를 Mockito로 대체하므로 DB 설정과 무관하게 서비스의 판단만 확인할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class HqStockRequestServiceTest {

    // 서비스가 의존하는 저장소는 가짜 객체로 주입하고, 필요한 반환값만 테스트별로 지정한다.
    @Mock
    private StockRequestRepository stockRequestRepository;
    @Mock
    private StockRequestItemRepository stockRequestItemRepository;

    @InjectMocks
    private HqStockRequestService service;

    @Test
    // 승인하면 각 품목의 요청 수량 전체가 승인 수량으로 복사되는지 확인한다.
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

        // 실행(When): PENDING 신청을 본사 관리자가 승인한다.
        StockRequestResponse response = service.approveStockRequest(hq, 50L);

        // 검증(Then): 품목 수량과 처리 담당자·시간이 함께 기록되어야 한다.
        assertEquals(StockRequestStatus.PREPARING, response.requestStatus());
        assertEquals(2, first.getApprovedQuantity());
        assertEquals(5, second.getApprovedQuantity());
        assertSame(hq, stockRequest.getProcessedAdmin());
        assertNotNull(stockRequest.getProcessedAt());
    }

    @Test
    // 검색어 앞뒤 공백을 제거하고 LIKE 검색용 패턴으로 바꾸어 Repository에 전달하는지 확인한다.
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

        // 실행 시 사용자가 입력한 "  강남  "이 "%강남%"으로 정규화된다.
        Page<StockRequestResponse> result = service.getStockRequests(
                hq, StockRequestStatus.PENDING, 1L, from, to, "  강남  ", pageable);

        // 반환된 페이지 내용과 Repository에 전달된 실제 인자를 모두 확인한다.
        assertEquals(1, result.getTotalElements());
        assertEquals(50L, result.getContent().getFirst().stockRequestId());
        verify(stockRequestRepository).searchForHq(
                StockRequestStatus.PENDING, 1L, from, to, "%강남%", pageable);
    }

    @Test
    // 반려 시 상태뿐 아니라 사유, 처리자, 처리 시간이 함께 기록되는지 확인한다.
    void rejectRecordsReasonAndProcessor() {
        Admin hq = hqAdmin();
        Branch branch = branch();
        StockRequest stockRequest = stockRequest(
                branch,
                branchManager(branch),
                StockRequestStatus.PENDING);
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));
        when(stockRequestItemRepository.findByStockRequest_StockRequestId(50L)).thenReturn(List.of());

        StockRequestResponse response = service.rejectStockRequest(
                hq,
                50L,
                new RejectRequest("본점 재고 부족"));

        assertEquals(StockRequestStatus.REJECTED, response.requestStatus());
        assertEquals("본점 재고 부족", stockRequest.getRejectionReason());
        assertSame(hq, stockRequest.getProcessedAdmin());
        assertNotNull(stockRequest.getProcessedAt());
    }

    @Test
    // 배송 시작 시 송장번호와 택배·기사·도착예정 정보가 저장되는지 확인한다.
    void shipRecordsDeliveryInformation() {
        Admin hq = hqAdmin();
        Branch branch = branch();
        LocalDateTime estimatedArrivalAt = LocalDateTime.of(2026, 7, 15, 14, 0);
        StockRequest stockRequest = stockRequest(
                branch,
                branchManager(branch),
                StockRequestStatus.PREPARING);
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));
        when(stockRequestItemRepository.findByStockRequest_StockRequestId(50L)).thenReturn(List.of());

        StockRequestResponse response = service.shipStockRequest(
                hq,
                50L,
                new ShipRequest("TRACK-100", "기본택배", "홍길동", estimatedArrivalAt));

        assertEquals(StockRequestStatus.SHIPPING, response.requestStatus());
        assertEquals("TRACK-100", stockRequest.getTrackingNumber());
        assertEquals("기본택배", stockRequest.getCourierName());
        assertEquals("홍길동", stockRequest.getDriverName());
        assertEquals(estimatedArrivalAt, stockRequest.getEstimatedArrivalAt());
        assertNotNull(stockRequest.getShippedAt());
    }

    @Test
    // PENDING이 아닌 신청을 다시 승인하면 상태 충돌로 거절되는지 확인한다.
    void approveRejectsNonPendingRequest() {
        Admin hq = hqAdmin();
        Branch branch = branch();
        StockRequest stockRequest = stockRequest(
                branch,
                branchManager(branch),
                StockRequestStatus.PREPARING);
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.approveStockRequest(hq, 50L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    // PENDING이 아닌 신청은 반려할 수도 없다는 상태 전이 규칙을 확인한다.
    void rejectRejectsNonPendingRequest() {
        Admin hq = hqAdmin();
        Branch branch = branch();
        StockRequest stockRequest = stockRequest(
                branch,
                branchManager(branch),
                StockRequestStatus.PREPARING);
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.rejectStockRequest(hq, 50L, new RejectRequest("반려 사유")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    // 물품 준비가 끝나지 않은 PENDING 신청은 배송을 시작할 수 없는지 확인한다.
    void shipRejectsNonPreparingRequest() {
        Admin hq = hqAdmin();
        Branch branch = branch();
        StockRequest stockRequest = stockRequest(
                branch,
                branchManager(branch),
                StockRequestStatus.PENDING);
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.shipStockRequest(
                        hq,
                        50L,
                        new ShipRequest("TRACK-100", null, null, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    // 지점 관리자가 본사 전용 요약 기능을 사용할 수 없는지 역할 권한을 확인한다.
    void hqServiceRejectsBranchManager() {
        Branch branch = branch();
        Admin branchManager = branchManager(branch);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getSummary(branchManager));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    /** 지점, 신청자, 상태만 바꾸어 재사용할 수 있는 신청 테스트 데이터를 만든다. */
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

    /** 신청서에 속하는 한 개 품목 테스트 데이터를 만든다. */
    private StockRequestItem item(StockRequest request, Flavor flavor, int quantity) {
        return StockRequestItem.builder()
                .stockRequest(request)
                .flavor(flavor)
                .requestedQuantity(quantity)
                .build();
    }

    /** 테스트가 공통으로 사용하는 지점 데이터를 만든다. */
    private Branch branch() {
        return Branch.builder()
                .branchId(1L)
                .branchName("강남점")
                .address("서울")
                .build();
    }

    /** 지정한 지점에 소속된 지점 관리자를 만든다. */
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

    /** 본사 전용 기능을 호출할 HQ_ADMIN 테스트 계정을 만든다. */
    private Admin hqAdmin() {
        return Admin.builder()
                .adminId(20L)
                .loginId("hq")
                .passwordHash("pw")
                .name("본점 관리자")
                .role(AdminRole.HQ_ADMIN)
                .build();
    }

    /** ID와 이름만 가진 최소 맛 테스트 데이터를 만든다. */
    private Flavor flavor(long id, String name) {
        return Flavor.builder()
                .flavorId(id)
                .flavorName(name)
                .build();
    }
}
