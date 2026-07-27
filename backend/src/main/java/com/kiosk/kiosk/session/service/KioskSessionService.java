package com.kiosk.kiosk.session.service;

import com.kiosk.domain.branch.Branch;
import com.kiosk.domain.branch.BranchRepository;
import com.kiosk.domain.branch.KioskStatus;
import com.kiosk.kiosk.session.dto.KioskDailySummaryResponse;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class KioskSessionService {

    private static final String PAID_STATUSES = "('PAID','MAKING','READY','COMPLETED')";

    private final BranchRepository branchRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public KioskDailySummaryResponse getTodaySummary(Long branchId) {
        requireBranch(branchId);
        LocalDate today = LocalDate.now();

        // 로그아웃 팝업에는 오늘 생성되고 결제까지 완료된 주문만 합산한다.
        // PENDING_PAYMENT/CANCELLED 주문은 실제 매출이 아니므로 건수와 금액 모두에서 제외한다.
        Map<String, Object> summary = jdbcTemplate.queryForMap("""
                SELECT COALESCE(SUM(final_amount), 0) revenue, COUNT(*) order_count
                FROM `order`
                WHERE branch_id=? AND order_status IN """ + PAID_STATUSES + """
                  AND created_at >= ? AND created_at < ?""",
                branchId, Date.valueOf(today), Date.valueOf(today.plusDays(1)));

        // 아이스크림은 싱글/패밀리 같은 크기가 아니라 실제 선택한 맛을 집계한다.
        // 맛 선택 행이 없는 커피·음료 등은 상품명을 사용하고, UNION ALL 결과를 합쳐 전체 인기 TOP 5를 만든다.
        List<KioskDailySummaryResponse.PopularMenu> popularMenus = jdbcTemplate.query("""
                SELECT sold_name product_name, SUM(quantity) quantity
                FROM (
                    SELECT oif.flavor_name_snapshot sold_name, SUM(oif.quantity) quantity
                    FROM order_item_flavor oif
                    JOIN order_item oi ON oi.order_item_id=oif.order_item_id
                    JOIN `order` o ON o.order_id=oi.order_id
                    WHERE o.branch_id=? AND o.order_status IN """ + PAID_STATUSES + """
                      AND o.created_at >= ? AND o.created_at < ?
                    GROUP BY oif.flavor_name_snapshot

                    UNION ALL

                    SELECT oi.product_name_snapshot sold_name, SUM(oi.quantity) quantity
                    FROM order_item oi
                    JOIN `order` o ON o.order_id=oi.order_id
                    WHERE o.branch_id=? AND o.order_status IN """ + PAID_STATUSES + """
                      AND o.created_at >= ? AND o.created_at < ?
                      AND NOT EXISTS (
                          SELECT 1 FROM order_item_flavor oif
                          WHERE oif.order_item_id=oi.order_item_id
                      )
                    GROUP BY oi.product_name_snapshot
                ) sold
                GROUP BY sold_name
                ORDER BY quantity DESC, product_name ASC
                LIMIT 5""",
                (rs, rowNum) -> new KioskDailySummaryResponse.PopularMenu(
                        rs.getString("product_name"), rs.getInt("quantity")),
                branchId, Date.valueOf(today), Date.valueOf(today.plusDays(1)),
                branchId, Date.valueOf(today), Date.valueOf(today.plusDays(1)));

        return new KioskDailySummaryResponse(
                ((Number) summary.get("revenue")).intValue(),
                ((Number) summary.get("order_count")).intValue(),
                popularMenus);
    }

    @Transactional
    public void start(Long branchId) {
        Branch branch = requireBranch(branchId);
        // 영업 종료 후 다음 날 또는 재등록 시 키오스크가 다시 주문을 받을 수 있도록 활성 상태로 복구한다.
        branch.setKioskStatus(KioskStatus.ACTIVE);
        branch.setKioskLastAccessAt(LocalDateTime.now());
    }

    @Transactional
    public void logout(Long branchId, boolean closeBusiness) {
        Branch branch = requireBranch(branchId);
        // 영업 종료는 기기 상태까지 INACTIVE로 저장하고, 단순 로그아웃은 매출 데이터만 그대로 둔 채 상태를 유지한다.
        if (closeBusiness) {
            branch.setKioskStatus(KioskStatus.INACTIVE);
        }
        branch.setKioskLastAccessAt(LocalDateTime.now());
    }

    private Branch requireBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "지점을 찾을 수 없습니다."));
    }
}
