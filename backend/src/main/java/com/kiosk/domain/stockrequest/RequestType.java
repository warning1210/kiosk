package com.kiosk.domain.stockrequest;

/**
 * 재고 신청을 만든 목적을 구분한다.
 *
 * <p>문자열로 DB에 저장되므로 각 상수 이름은 이미 저장된 데이터와의 약속이다. 상수 이름을 바꿀 때는
 * 애플리케이션 코드뿐 아니라 기존 DB 값도 함께 마이그레이션해야 한다.
 */
public enum RequestType {
    /** 부족한 상품을 본사에 요청하여 지점 재고를 채우는 일반적인 재입고 신청이다. */
    RESTOCK,

    /** 실사 결과처럼 장부와 실제 재고의 차이를 맞추기 위한 조정 신청이다. */
    ADJUSTMENT
}
