package com.example.kiosksim.dto;

import java.util.List;

public record CatalogResponse(
        List<ProductResponse> products,
        List<FlavorResponse> flavors
) {
}
