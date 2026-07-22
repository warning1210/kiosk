package com.kiosk.domain.event;

// MONTHLY_FLAVOR: 본점이 할인 맛을 직접 지정(이달의 맛) - 전 지점에 자동 적용, 지점이 관여하지 않음
// FLAVOR_DISCOUNT: 본점은 할인값(율/금액)만 정하고, 어느 맛에 붙일지는 지점이 event_branch_flavor로 선택
// SIZE_UP: 본점이 상품A -> 상품B 사이즈업 + 추가금액을 지정 - 전 지점에 자동 적용
public enum EventType {
    COUPON,
    FLAVOR_DISCOUNT,
    SIZE_UP,
    MONTHLY_FLAVOR
}
