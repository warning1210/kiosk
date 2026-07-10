package com.kiosk.kiosk.menu.dto;

import com.kiosk.domain.flavor.Flavor;
import com.kiosk.domain.common.SaleStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuFlavorDto {
    private Long flavorId;
    private String flavorName;
    private String imageUrl;
    private String description;
    private String allergyInfo;
    private SaleStatus saleStatus;

    public static MenuFlavorDto from(Flavor flavor) {
        return MenuFlavorDto.builder()
                .flavorId(flavor.getFlavorId())
                .flavorName(flavor.getFlavorName())
                .imageUrl(flavor.getImageUrl())
                .description(flavor.getDescription())
                .allergyInfo(flavor.getAllergyInfo())
                .saleStatus(flavor.getSaleStatus())
                .build();
    }
}
