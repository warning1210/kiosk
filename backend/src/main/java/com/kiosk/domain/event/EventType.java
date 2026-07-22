package com.kiosk.domain.event;

// MONTHLY_FLAVOR: 본점이 맛을 직접 지정(이달의 맛) - 할인은 없고, 이 맛을 고르면 사이즈업(상품A -> 상품B +
// 추가금액)이 반드시 같이 걸린다 - 전 지점 자동 적용, 지점 관여 없음. (실제 배스킨라빈스 "이 맛 고르면
// 싱글레귤러를 더블주니어로 사이즈업" 프로모션과 동일)
// HQ_FLAVOR_DISCOUNT: 본점이 맛과 할인값(율/금액)을 둘 다 직접 지정 - 전 지점 자동 적용, 지점 관여 없음.
// FLAVOR_DISCOUNT: 본점은 할인값(율/금액)만 정하고, 어느 맛에 붙일지는 지점이 event_branch_flavor로 선택
public enum EventType {
    MONTHLY_FLAVOR,
    HQ_FLAVOR_DISCOUNT,
    FLAVOR_DISCOUNT
}
