package com.example.kiosksim.controller;

import com.example.kiosksim.dto.CatalogResponse;
import com.example.kiosksim.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public CatalogResponse catalog() {
        return catalogService.getCatalog();
    }
}
