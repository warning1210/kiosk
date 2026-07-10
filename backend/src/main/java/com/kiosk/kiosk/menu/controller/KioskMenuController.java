package com.kiosk.kiosk.menu.controller;

import com.kiosk.kiosk.menu.dto.MenuCategoryDto;
import com.kiosk.kiosk.menu.service.KioskMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kiosk")
public class KioskMenuController {

    private final KioskMenuService kioskMenuService;

    @GetMapping("/{branchId}/menus")
    public ResponseEntity<List<MenuCategoryDto>> getKioskMenus(@PathVariable Long branchId) {
        List<MenuCategoryDto> menus = kioskMenuService.getKioskMenus(branchId);
        return ResponseEntity.ok(menus);
    }
}
