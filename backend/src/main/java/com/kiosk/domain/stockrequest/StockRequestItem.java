package com.kiosk.domain.stockrequest;

import com.kiosk.domain.flavor.Flavor;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRequestItem {

    private Long stockRequestItemId;

    private StockRequest stockRequest;

    private Flavor flavor;

    private Integer requestedQuantity;

    private Integer approvedQuantity;

    private LocalDateTime createdAt;

    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 蹂몄궗媛 ?뱀씤?섎㈃???ㅼ젣濡?蹂대궡 以??섎웾???뺤젙?쒕떎. */
    public void approve(int approvedQuantity) {
        this.approvedQuantity = approvedQuantity;
    }

    /**
     * 吏?먯씠 ?ㅼ젣濡?諛쏄쾶 ???섎웾(???⑥쐞).
     * 蹂몄궗媛 ?뱀씤 ?섎웾???곕줈 吏?뺥뻽?쇰㈃ 洹?媛믪쓣, ?꾨땲硫?吏?먯씠 ?좎껌???섎웾??洹몃?濡??대떎.
     */
    public int getQuantityToReceive() {
        return approvedQuantity != null ? approvedQuantity : requestedQuantity;
    }
}
