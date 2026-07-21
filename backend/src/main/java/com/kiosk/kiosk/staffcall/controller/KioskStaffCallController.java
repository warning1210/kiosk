package com.kiosk.kiosk.staffcall.controller;

import com.kiosk.kiosk.staffcall.dto.StaffCallRequest;
import com.kiosk.kiosk.staffcall.service.KioskStaffCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KioskStaffCallController {

    private final KioskStaffCallService kioskStaffCallService;

    @PostMapping("/staff-call")
    public void call(@RequestBody StaffCallRequest request) {
        kioskStaffCallService.call(request.branchId());
    }
}
