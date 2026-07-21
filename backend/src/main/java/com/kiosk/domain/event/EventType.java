package com.kiosk.domain.event;

// COUPON: 본점이 쿠폰을 배부하는 이벤트
// FLAVOR_DISCOUNT: 특정 상품(맛)에 할인을 붙이는 이벤트 - 어느 맛에 붙일지는 지점이 event_branch_flavor로 선택
public enum EventType {
    COUPON,
    FLAVOR_DISCOUNT
}
