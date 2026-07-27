package com.kiosk.kiosk.session.controller;

import com.kiosk.kiosk.session.dto.KioskDailySummaryResponse;
import com.kiosk.kiosk.session.service.KioskSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class KioskSessionController {

    private final KioskSessionService kioskSessionService;

    @GetMapping("/api/kiosk/session/today-summary")
    public KioskDailySummaryResponse getTodaySummary(@RequestParam Long branchId) {
        return kioskSessionService.getTodaySummary(branchId);
    }

    @PatchMapping("/api/kiosk/session/start")
    public ResponseEntity<Void> start(@RequestParam Long branchId) {
        kioskSessionService.start(branchId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/kiosk/session/logout")
    public ResponseEntity<Void> logout(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "false") boolean closeBusiness) {
        kioskSessionService.logout(branchId, closeBusiness);
        return ResponseEntity.noContent().build();
    }
}
