package com.kiosk.branch.stockrequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestItemRequest;
import com.kiosk.branch.stockrequest.dto.StockRequestResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BranchStockRequestServiceTest {

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
                () -> service.create(manager, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(flavorRepository);
        verify(stockRequestRepository, never()).save(any());
        verify(stockRequestItemRepository, never()).saveAll(any());
    }

    @Test
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

        StockRequestResponse response = service.confirmReceipt(manager, 50L);

        assertEquals(StockRequestStatus.DELIVERED, response.requestStatus());
        assertEquals(10, inventory.getCurrentQuantity());
        assertEquals(InventoryStatus.LOW, inventory.getInventoryStatus());
        assertSame(manager, stockRequest.getReceiptConfirmedAdmin());
        assertNotNull(stockRequest.getDeliveredAt());

        ArgumentCaptor<InventoryTransaction> captor = ArgumentCaptor.forClass(InventoryTransaction.class);
        verify(inventoryTransactionRepository).save(captor.capture());
        InventoryTransaction transaction = captor.getValue();
        assertEquals(InventoryTransactionType.REQUEST_RECEIVED, transaction.getTransactionType());
        assertEquals(3, transaction.getChangeQuantity());
        assertEquals(10, transaction.getQuantityAfter());
        assertSame(inventory, transaction.getBranchInventory());
        assertSame(stockRequest, transaction.getStockRequest());
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

    private Flavor flavor(long id, String name) {
        return Flavor.builder()
                .flavorId(id)
                .flavorName(name)
                .build();
    }
}
