package com.kiosk.kiosk.menu;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/products")
    public List<ProductResponse> getProducts() {
        return menuService.getProducts();
    }

    @GetMapping("/flavors")
    public List<FlavorResponse> getFlavors() {
        return menuService.getFlavors();
    }
}
