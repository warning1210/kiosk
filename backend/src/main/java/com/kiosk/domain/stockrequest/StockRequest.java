package com.kiosk.domain.stockrequest;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.branch.Branch;
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
public class StockRequest {

    private Long stockRequestId;

    private String requestNumber;

    private Branch branch;

    private Admin requesterAdmin;

    @Builder.Default
    private RequestType requestType = RequestType.RESTOCK;

    @Builder.Default
    private StockRequestStatus requestStatus = StockRequestStatus.PENDING;

    private String requestReason;

    private String rejectionReason;

    @Builder.Default
    private Urgency urgency = Urgency.NORMAL;

    private LocalDateTime requestedAt;

    private Admin processedAdmin;

    private LocalDateTime processedAt;

    private String shipmentNumber;

    private String trackingNumber;

    private String courierName;

    private String driverName;

    private LocalDateTime estimatedArrivalAt;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    private Admin receiptConfirmedAdmin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ?꾨옒???좎껌 泥섎━ ?먮쫫(?좎껌 -> ?뱀씤/諛섎젮 -> 諛곗넚 -> ?섎졊?뺤씤)?먯꽌 ???④퀎媛 ?섏뼱媛???    // ?④퍡 諛붾뚯뼱???섎뒗 ?꾨뱶?ㅼ쓣 ?섎굹濡?臾띠? 硫붿꽌?쒕떎. setter瑜??щ윭 踰??몄텧?섎뒗 ?????硫붿꽌?쒕? ?곕㈃
    // "?대뒓 ?④퀎濡??섏뼱媛??以묒씤吏"媛 ?몄텧遺?먯꽌 諛붾줈 ?쏀엺??
    // 吏湲??곹깭?먯꽌 洹??④퀎濡??섏뼱媛???섎뒗吏?????寃?щ뒗 Service媛 ?대떦?쒕떎.

    /** PK媛 留뚮뱾?댁쭊 ?ㅼ뿉 REQ-?좎쭨-PK ?뺥깭???낅Т???좎껌踰덊샇瑜??뺤젙?쒕떎. */
    public void assignRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    /** 吏?먯씠 ?꾩쭅 泥섎━?섏? ?딆? ?좎껌???ㅼ뒪濡?痍⑥냼?쒕떎. */
    public void cancel() {
        this.requestStatus = StockRequestStatus.CLOSED;
    }

    /** 蹂몄궗媛 ?뱀씤?섎㈃ 怨㏓컮濡?異쒓퀬 以鍮?PREPARING) ?④퀎濡??섏뼱媛꾨떎. */
    public void approve(Admin admin, LocalDateTime processedAt) {
        this.requestStatus = StockRequestStatus.PREPARING;
        this.processedAdmin = admin;
        this.processedAt = processedAt;
    }

    /** 蹂몄궗媛 ?ъ쑀? ?④퍡 諛섎젮?쒕떎. */
    public void reject(Admin admin, String rejectionReason, LocalDateTime processedAt) {
        this.requestStatus = StockRequestStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.processedAdmin = admin;
        this.processedAt = processedAt;
    }

    /**
     * 蹂몄궗媛 異쒓퀬 泥섎━瑜??섎㈃??諛곗넚以?SHIPPING) ?곹깭濡??꾪솚?쒕떎.
     *
     * <p>?앸같?ъ뿉 留↔린??寃??꾨땲??蹂몄궗媛 吏곸젒 諛곗넚?섎뒗 援ъ“?? ?댁넚?λ쾲???앸같??諛쒓툒)???앸같?щ챸?
     * ?곗? ?딅뒗?? ???蹂몄궗 ?대? 諛곗넚踰덊샇({@code shipmentNumber})瑜??쒖뒪?쒖씠 ?먮룞?쇰줈 諛쒓툒?섍퀬,
     * 諛곗넚?대떦??{@code driverName})??蹂몄궗 ?대떦?먭? 吏곸젒 ?낅젰?쒕떎.
     */
    public void dispatch(String shipmentNumber, String driverName, LocalDateTime estimatedArrivalAt,
            LocalDateTime shippedAt) {
        this.requestStatus = StockRequestStatus.SHIPPING;
        this.shipmentNumber = shipmentNumber;
        this.driverName = driverName;
        this.estimatedArrivalAt = estimatedArrivalAt;
        this.shippedAt = shippedAt;
    }

    /** 吏?먯씠 ?ㅻЪ ?섎졊???뺤씤?쒕떎. ?ㅼ젣 ?ш퀬 諛섏쁺? 吏???ш퀬 ?붾㈃???낃퀬 泥섎━?먯꽌 ?대쨪吏꾨떎. */
    public void confirmReceipt(Admin admin, LocalDateTime deliveredAt) {
        this.requestStatus = StockRequestStatus.DELIVERED;
        this.receiptConfirmedAdmin = admin;
        this.deliveredAt = deliveredAt;
    }
}
