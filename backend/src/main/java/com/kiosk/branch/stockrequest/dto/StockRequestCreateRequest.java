package com.kiosk.branch.stockrequest.dto;

import com.kiosk.domain.stockrequest.Urgency;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 지점이 새 재고 신청을 만들 때 보내는 JSON 본문의 형태다.
 *
 * <p>요청 DTO를 {@code record}로 만들면 각 필드를 다시 대입하는 setter 없이 접근자와
 * 생성자가 자동으로 제공된다. 단, record가 목록 내부까지 깊은 불변성을 보장하는 것은 아니다.
 * {@code items}의 {@link Valid @Valid}는 목록 안의 각 {@link StockRequestItemRequest}
 * 검증까지 이어지게 한다.</p>
 *
 * @param requestReason 재고가 필요한 이유. 입력하지 않을 수도 있다.
 * @param urgency 신청 긴급도. 생략하면 서비스에서 보통({@code NORMAL})으로 정한다.
 * @param items 하나 이상이어야 하는 맛별 신청 품목 목록
 */
public record StockRequestCreateRequest(
        String requestReason,
        Urgency urgency,
        @NotEmpty(message = "신청할 상품을 1개 이상 선택해주세요") @Valid List<StockRequestItemRequest> items
) {
}
