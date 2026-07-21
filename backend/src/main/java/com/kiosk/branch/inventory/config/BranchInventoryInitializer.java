package com.kiosk.branch.inventory.config;

import com.kiosk.branch.inventory.service.BranchInventoryService;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.branch.OperationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// 기능 적용 전에 가입한 기존 운영 지점도 누락된 맛만 만재 상태로 보충한다.
@Component
@RequiredArgsConstructor
public class BranchInventoryInitializer implements CommandLineRunner {

    private final BranchRepository branchRepository;
    private final BranchInventoryService branchInventoryService;

    @Override
    public void run(String... args) {
        branchRepository.findAll().stream()
                // 승인되어 실제 운영 중인 지점만 초기 재고 보충 대상으로 삼는다.
                .filter(branch -> branch.getOperationStatus() == OperationStatus.ACTIVE)
                // 서비스가 이미 존재하는 재고 행은 건너뛰므로 마지막 주문 수량이 보존된다.
                .forEach(branchInventoryService::initializeMissingInventory);
    }
}
