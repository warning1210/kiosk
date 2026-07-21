package com.kiosk.kiosk.staffcall.service;

import com.kiosk.global.staffcall.StaffCallRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KioskStaffCallService {

    private final StaffCallRegistry staffCallRegistry;

    public void call(Long branchId) {
        staffCallRegistry.call(branchId);
    }
}
