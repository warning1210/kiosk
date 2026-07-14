package com.kiosk.hq.stockrequest;

import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.CurrentAdmin;
import com.kiosk.hq.stockrequest.dto.RejectRequest;
import com.kiosk.hq.stockrequest.dto.ShipRequest;
import com.kiosk.hq.stockrequest.dto.StockRequestSummaryResponse;
import com.kiosk.stockrequest.dto.StockRequestResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 본사 관리자가 지점 재고 신청을 조회하고 처리할 때 사용하는 HTTP 진입점이다.
 *
 * <p>쿼리 파라미터와 요청 본문을 Java 값으로 변환한 뒤
 * {@link HqStockRequestService}에 전달하는 역할만 맡는다. 본사 권한 확인과 상태 전환
 * 가능 여부는 서비스에서 일관되게 검사한다.</p>
 */
@RestController
@RequestMapping("/api/hq/stock-requests")
public class HqStockRequestController {

    /** 본사 관점의 검색, 집계, 승인·반려·배송 업무를 수행하는 서비스다. */
    private final HqStockRequestService hqStockRequestService;

    /**
     * 생성자 주입으로 본사 재고 신청 서비스를 받는다.
     *
     * @param hqStockRequestService 본사 재고 신청 업무 서비스
     */
    public HqStockRequestController(HqStockRequestService hqStockRequestService) {
        this.hqStockRequestService = hqStockRequestService;
    }

    /**
     * 여러 선택 조건을 조합해 지점들의 재고 신청을 페이지 단위로 조회한다.
     *
     * <p>{@code from}과 {@code to}는 ISO 날짜-시간 문자열을 {@link LocalDateTime}으로
     * 변환한다. 모든 검색 조건은 선택 사항이므로 필요한 값만 보낼 수 있다.</p>
     *
     * @param admin 현재 로그인한 본사 관리자
     * @param status 선택적인 신청 상태
     * @param branchId 선택적인 지점 기본키
     * @param from 선택적인 신청 시작 시각
     * @param to 선택적인 신청 종료 시각
     * @param keyword 선택적인 신청 번호·지점명·맛 이름 검색어
     * @param pageable 페이지 번호, 크기, 정렬 정보
     * @return 검색 조건을 만족하는 재고 신청 응답 페이지
     */
    @GetMapping
    public Page<StockRequestResponse> getStockRequests(
            @CurrentAdmin Admin admin,
            @RequestParam(required = false) StockRequestStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return hqStockRequestService.getStockRequests(admin, status, branchId, from, to, keyword, pageable);
    }

    /**
     * 본사 대시보드에 표시할 상태별 신청 건수를 조회한다.
     *
     * @param admin 현재 로그인한 본사 관리자
     * @return 전체·대기·승인 후 정상 처리·반려 건수
     */
    @GetMapping("/summary")
    public StockRequestSummaryResponse getSummary(@CurrentAdmin Admin admin) {
        return hqStockRequestService.getSummary(admin);
    }

    /**
     * 대기 중인 신청을 승인하고 모든 품목의 승인 수량을 확정한다.
     *
     * @param admin 승인하는 현재 본사 관리자
     * @param id 승인할 신청 기본키
     * @return 승인 후 신청 정보
     */
    @PatchMapping("/{id}/approve")
    public StockRequestResponse approveStockRequest(@CurrentAdmin Admin admin, @PathVariable Long id) {
        return hqStockRequestService.approveStockRequest(admin, id);
    }

    /**
     * 대기 중인 신청을 사유와 함께 반려한다.
     *
     * <p>{@code @Valid}가 빈 반려 사유를 서비스 호출 전에 차단한다.</p>
     *
     * @param admin 반려하는 현재 본사 관리자
     * @param id 반려할 신청 기본키
     * @param request 반드시 사유가 포함된 반려 요청
     * @return 반려 후 신청 정보
     */
    @PatchMapping("/{id}/reject")
    public StockRequestResponse rejectStockRequest(
            @CurrentAdmin Admin admin,
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request) {
        return hqStockRequestService.rejectStockRequest(admin, id, request);
    }

    /**
     * 준비 중인 신청에 운송 정보를 기록하고 배송 중 상태로 전환한다.
     *
     * @param admin 배송을 등록하는 현재 본사 관리자
     * @param id 배송할 신청 기본키
     * @param request 운송장 번호와 선택적인 배송 기사·도착 예정 정보
     * @return 배송 등록 후 신청 정보
     */
    @PatchMapping("/{id}/ship")
    public StockRequestResponse shipStockRequest(
            @CurrentAdmin Admin admin,
            @PathVariable Long id,
            @Valid @RequestBody ShipRequest request) {
        return hqStockRequestService.shipStockRequest(admin, id, request);
    }
}
