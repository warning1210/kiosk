package com.kiosk.branch.stockrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestItemRequest;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.admin.AdminRole;
import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.flavor.FlavorRepository;
import com.kiosk.domain.inventory.BranchInventory;
import com.kiosk.domain.inventory.BranchInventoryRepository;
import com.kiosk.domain.inventory.InventoryStatus;
import com.kiosk.domain.inventory.InventoryTransaction;
import com.kiosk.domain.inventory.InventoryTransactionRepository;
import com.kiosk.domain.inventory.InventoryTransactionType;
import com.kiosk.domain.stockrequest.StockRequest;
import com.kiosk.domain.stockrequest.StockRequestItem;
import com.kiosk.domain.stockrequest.StockRequestItemRepository;
import com.kiosk.domain.stockrequest.StockRequestRepository;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.domain.stockrequest.Urgency;
import com.kiosk.stockrequest.dto.StockRequestResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * 지점 재고신청 서비스의 규칙을 실제 DB 없이 빠르게 확인하는 단위 테스트다.
 *
 * <p>Repository는 Mockito 가짜 객체로 대체하고, 각 테스트는
 * 준비(Given) → 실행(When) → 검증(Then) 순서로 읽을 수 있게 구성한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class BranchStockRequestServiceTest {

    // 서비스가 협력하는 저장소를 가짜 객체로 만들어, 테스트할 규칙에만 집중한다.
    @Mock
    private StockRequestRepository stockRequestRepository;
    @Mock
    private StockRequestItemRepository stockRequestItemRepository;
    @Mock
    private FlavorRepository flavorRepository;
    @Mock
    private BranchInventoryRepository branchInventoryRepository;
    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private BranchStockRequestService service;

    @Test
    // 같은 맛을 두 번 신청하면 품목을 저장하기 전에 입력 오류로 막는지 확인한다.
    void createRejectsDuplicateFlavorIds() {
        Branch branch = branch();
        Admin manager = branchManager(branch);
        StockRequestCreateRequest request = new StockRequestCreateRequest(
                "재고 부족",
                Urgency.NORMAL,
                List.of(
                        new StockRequestItemRequest(100L, 2),
                        new StockRequestItemRequest(100L, 3)
                ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createStockRequest(manager, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(flavorRepository);
        verify(stockRequestRepository, never()).save(any());
        verify(stockRequestItemRepository, never()).saveAll(any());
    }

    @Test
    // DB에 없는 맛 ID가 들어오면 신청서와 품목을 저장하지 않는지 확인한다.
    void createRejectsUnknownFlavor() {
        Branch branch = branch();
        Admin manager = branchManager(branch);
        StockRequestCreateRequest request = new StockRequestCreateRequest(
                "재고 부족",
                Urgency.NORMAL,
                List.of(new StockRequestItemRequest(999L, 2)));
        when(flavorRepository.findAllById(List.of(999L))).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createStockRequest(manager, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(stockRequestRepository, never()).save(any());
        verify(stockRequestItemRepository, never()).saveAll(any());
    }

    @Test
    // 최초 INSERT에 쓰는 임시 신청번호가 request_number 컬럼의 30자 제한을 넘지 않는지 확인한다.
    void createUsesTemporaryRequestNumberWithinColumnLength() {
        Branch branch = branch();
        Admin manager = branchManager(branch);
        Flavor vanilla = flavor(100L, "바닐라");
        StockRequestCreateRequest request = new StockRequestCreateRequest(
                "재고 부족",
                Urgency.NORMAL,
                List.of(new StockRequestItemRequest(100L, 2)));
        AtomicReference<String> temporaryRequestNumber = new AtomicReference<>();

        when(flavorRepository.findAllById(List.of(100L))).thenReturn(List.of(vanilla));
        when(stockRequestRepository.save(any(StockRequest.class))).thenAnswer(invocation -> {
            StockRequest savedRequest = invocation.getArgument(0);
            // save() 시점의 임시 값을 보관하고, DB의 IDENTITY PK 발급만 테스트에서 흉내 낸다.
            temporaryRequestNumber.set(savedRequest.getRequestNumber());
            ReflectionTestUtils.setField(savedRequest, "stockRequestId", 50L);
            return savedRequest;
        });

        StockRequestResponse response = service.createStockRequest(manager, request);

        assertNotNull(temporaryRequestNumber.get());
        assertEquals(30, temporaryRequestNumber.get().length());
        assertTrue(temporaryRequestNumber.get().startsWith("TMP-"));
        assertTrue(response.requestNumber().endsWith("-50"));
        verify(stockRequestItemRepository).saveAll(any());
    }

    @Test
    // 로그인 점장이 다른 지점의 신청을 취소할 수 없는지 확인한다.
    void cancelRejectsRequestOwnedByAnotherBranch() {
        Branch managerBranch = branch();
        Branch anotherBranch = Branch.builder()
                .branchId(2L)
                .branchName("잠실점")
                .address("서울")
                .build();
        Admin manager = branchManager(managerBranch);
        StockRequest stockRequest = StockRequest.builder()
                .stockRequestId(50L)
                .branch(anotherBranch)
                .requestStatus(StockRequestStatus.PENDING)
                .build();
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelStockRequest(manager, 50L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(StockRequestStatus.PENDING, stockRequest.getRequestStatus());
    }

    @Test
    // 본사가 처리하기 시작한 PREPARING 신청은 지점에서 취소할 수 없는지 확인한다.
    void cancelRejectsNonPendingRequest() {
        Branch branch = branch();
        Admin manager = branchManager(branch);
        StockRequest stockRequest = StockRequest.builder()
                .stockRequestId(50L)
                .branch(branch)
                .requestStatus(StockRequestStatus.PREPARING)
                .build();
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancelStockRequest(manager, 50L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(StockRequestStatus.PREPARING, stockRequest.getRequestStatus());
    }

    @Test
    // 배송 전 단계의 신청을 입고 확정하지 못하도록 상태 순서를 검사한다.
    void confirmReceiptRejectsNonShippingRequest() {
        Branch branch = branch();
        Admin manager = branchManager(branch);
        StockRequest stockRequest = StockRequest.builder()
                .stockRequestId(50L)
                .branch(branch)
                .requestStatus(StockRequestStatus.PREPARING)
                .build();
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmStockReceipt(manager, 50L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(branchInventoryRepository, inventoryTransactionRepository);
    }

    @Test
    // 배송 중이어도 다른 지점 소유라면 입고 확정할 수 없는지 확인한다.
    void confirmReceiptRejectsRequestOwnedByAnotherBranch() {
        Branch managerBranch = branch();
        Branch anotherBranch = Branch.builder()
                .branchId(2L)
                .branchName("잠실점")
                .address("서울")
                .build();
        Admin manager = branchManager(managerBranch);
        StockRequest stockRequest = StockRequest.builder()
                .stockRequestId(50L)
                .branch(anotherBranch)
                .requestStatus(StockRequestStatus.SHIPPING)
                .build();
        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.confirmStockReceipt(manager, 50L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(branchInventoryRepository, inventoryTransactionRepository);
    }

    @Test
    // 본사 관리자가 지점 전용 서비스를 사용할 수 없는지 역할 권한을 검사한다.
    void branchServiceRejectsHqAdmin() {
        Admin hqAdmin = Admin.builder()
                .adminId(20L)
                .role(AdminRole.HQ_ADMIN)
                .build();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getStockRequests(hqAdmin, null, null));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(stockRequestRepository);
    }

    @Test
    // 입고 확정의 전체 결과인 재고, 상태, 담당자, 입출고 이력을 한 번에 확인한다.
    // Mockito 단위 테스트이므로 잠금 조회 경로와 객체 결과를 확인하며, 실제 DB의 잠금 대기는 통합 테스트 대상이다.
    void confirmReceiptLocksInventoryAndTreatsSafetyQuantityAsLow() {
        Branch branch = branch();
        Admin manager = branchManager(branch);
        Flavor vanilla = flavor(100L, "바닐라");
        StockRequest stockRequest = StockRequest.builder()
                .stockRequestId(50L)
                .requestNumber("REQ-20260713-50")
                .branch(branch)
                .requesterAdmin(manager)
                .requestStatus(StockRequestStatus.SHIPPING)
                .urgency(Urgency.NORMAL)
                .requestedAt(LocalDateTime.now())
                .build();
        StockRequestItem item = StockRequestItem.builder()
                .stockRequest(stockRequest)
                .flavor(vanilla)
                .requestedQuantity(5)
                .approvedQuantity(3)
                .build();
        BranchInventory inventory = BranchInventory.builder()
                .branchInventoryId(70L)
                .branch(branch)
                .flavor(vanilla)
                .currentQuantity(7)
                .safetyQuantity(10)
                .inventoryStatus(InventoryStatus.LOW)
                .build();

        when(stockRequestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(stockRequest));
        when(stockRequestItemRepository.findByStockRequest_StockRequestId(50L)).thenReturn(List.of(item));
        when(branchInventoryRepository.findByBranchAndFlavorForUpdate(1L, 100L))
                .thenReturn(Optional.of(inventory));

        // 실행(When): SHIPPING 신청을 입고 확정한다.
        StockRequestResponse response = service.confirmStockReceipt(manager, 50L);

        // 검증(Then): 승인 수량 3이 더해져 10이 되고, 안전재고와 같으므로 LOW를 유지한다.
        assertEquals(StockRequestStatus.DELIVERED, response.requestStatus());
        assertEquals(10, inventory.getCurrentQuantity());
        assertEquals(InventoryStatus.LOW, inventory.getInventoryStatus());
        assertSame(manager, stockRequest.getReceiptConfirmedAdmin());
        assertNotNull(stockRequest.getDeliveredAt());

        // save()에 전달된 이력 객체를 꺼내 DB에 기록될 상세 값까지 검증한다.
        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository).save(captor.capture());
        InventoryTransaction transaction = captor.getValue();
        assertEquals(InventoryTransactionType.REQUEST_RECEIVED, transaction.getTransactionType());
        assertEquals(3, transaction.getChangeQuantity());
        assertEquals(10, transaction.getQuantityAfter());
        assertSame(inventory, transaction.getBranchInventory());
        assertSame(stockRequest, transaction.getStockRequest());
    }

    /** 여러 테스트가 공유하는 지점 데이터를 만드는 헬퍼다. */
    private Branch branch() {
        return Branch.builder()
                .branchId(1L)
                .branchName("강남점")
                .address("서울")
                .build();
    }

    /** 주어진 지점에 소속된 지점 관리자 데이터를 만드는 헬퍼다. */
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

    /** 테스트에 필요한 최소 맛 데이터를 만드는 헬퍼다. */
    private Flavor flavor(long id, String name) {
        return Flavor.builder()
                .flavorId(id)
                .flavorName(name)
                .build();
    }
}
