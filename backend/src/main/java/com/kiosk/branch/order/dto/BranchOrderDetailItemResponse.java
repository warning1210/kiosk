package com.kiosk.branch.order.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchOrderDetailItemResponse {
    private String productName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer itemTotal;
    private List<String> options;
}
