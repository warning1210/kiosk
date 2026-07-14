package com.kiosk.branch.stockrequest;

import com.kiosk.branch.stockrequest.dto.StockRequestCreateRequest;
import com.kiosk.domain.admin.Admin;
import com.kiosk.domain.stockrequest.StockRequestStatus;
import com.kiosk.global.security.CurrentAdmin;
import com.kiosk.stockrequest.dto.StockRequestResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지점 관리자가 사용하는 재고 신청 HTTP 요청을 받는 진입점이다.
 *
 * <p>컨트롤러는 요청 값의 형식 검증과 서비스 호출만 담당한다. 로그인한 관리자는
 * {@link CurrentAdmin}을 통해 전달받고, 소속 지점 확인·상태 변경 같은 업무 규칙은
 * {@link BranchStockRequestService}가 처리한다. 이렇게 역할을 나누면 HTTP 처리와
 * 핵심 업무 흐름을 각각 독립적으로 읽고 테스트할 수 있다.</p>
 */
@RestController
@RequestMapping("/api/branch/stock-requests")
public class BranchStockRequestController {

    /** 지점 재고 신청의 조회와 상태 변경을 실제로 수행하는 애플리케이션 서비스다. */
    private final BranchStockRequestService branchStockRequestService;

    /**
     * 생성자 주입으로 필요한 서비스를 받는다.
     *
     * @param branchStockRequestService 지점 재고 신청 업무를 처리할 서비스
     */
    public BranchStockRequestController(BranchStockRequestService branchStockRequestService) {
        this.branchStockRequestService = branchStockRequestService;
    }

    /**
     * 새 재고 신청을 등록한다.
     *
     * <p>{@code @Valid}가 요청 본문과 그 안의 신청 품목을 먼저 검증하므로,
     * 서비스에는 기본 형식이 확인된 값이 전달된다.</p>
     *
     * @param admin 현재 로그인한 지점 관리자
     * @param request 신청 사유, 긴급도, 신청 품목 목록
     * @return 저장된 신청 번호와 현재 상태를 포함한 응답
     */
    @PostMapping
    public StockRequestResponse createStockRequest(
            @CurrentAdmin Admin admin,
            @Valid @RequestBody StockRequestCreateRequest request) {
        return branchStockRequestService.createStockRequest(admin, request);
    }

    /**
     * 로그인한 관리자의 소속 지점 재고 신청을 페이지 단위로 조회한다.
     *
     * @param admin 현재 로그인한 지점 관리자
     * @param status 선택적인 상태 필터. 생략하면 모든 상태를 조회한다.
     * @param pageable 페이지 번호, 크기, 정렬 정보
     * @return 화면에 바로 전달할 수 있는 재고 신청 응답 페이지
     */
    @GetMapping
    public Page<StockRequestResponse> getStockRequests(
            @CurrentAdmin Admin admin,
            @RequestParam(required = false) StockRequestStatus status,
            Pageable pageable) {
        return branchStockRequestService.getStockRequests(admin, status, pageable);
    }

    /**
     * 아직 본사가 처리하지 않은 재고 신청을 취소한다.
     *
     * <p>정상 처리 후 본문이 필요 없으므로 HTTP 204(No Content)를 반환한다.</p>
     *
     * @param admin 현재 로그인한 지점 관리자
     * @param id 취소할 재고 신청의 기본키
     * @return 본문이 없는 204 응답
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelStockRequest(@CurrentAdmin Admin admin, @PathVariable Long id) {
        branchStockRequestService.cancelStockRequest(admin, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 배송 중인 신청의 실물 재고를 받았음을 확정한다.
     *
     * <p>이 호출은 신청 상태만 바꾸는 것이 아니라 지점 재고 수량과 재고 거래 이력도
     * 함께 갱신하므로, 자세한 일관성 처리는 서비스의 쓰기 트랜잭션 안에서 수행한다.</p>
     *
     * @param admin 현재 로그인한 지점 관리자
     * @param id 수령 확정할 재고 신청의 기본키
     * @return 수령 확정 후의 신청 정보와 품목 목록
     */
    @PatchMapping("/{id}/confirm-receipt")
    public StockRequestResponse confirmStockReceipt(@CurrentAdmin Admin admin, @PathVariable Long id) {
        return branchStockRequestService.confirmStockReceipt(admin, id);
    }
}
