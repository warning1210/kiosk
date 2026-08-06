package com.kiosk.kiosk.customer.dto;

import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.customer.CustomerGrade;
import java.util.List;

public record CustomerResponse(
        Long customerId,
        String mobileNumber,
        Integer pointBalance,
        CustomerGrade grade,
        List<CustomerCouponResponse> coupons
) {

    // Customer는 이제 평문 전화번호를 안 들고 있다 - 조회에 성공했다는 건 호출자가 넘긴 mobileNumber가
    // 이미 맞다는 뜻이라, DB 값을 복호화할 필요 없이 그 입력값을 그대로 돌려준다.
    public static CustomerResponse from(Customer customer, String mobileNumber, List<CustomerCouponResponse> coupons) {
        return new CustomerResponse(
                customer.getCustomerId(),
                mobileNumber,
                customer.getPointBalance(),
                customer.getGrade(),
                coupons
        );
    }
}
