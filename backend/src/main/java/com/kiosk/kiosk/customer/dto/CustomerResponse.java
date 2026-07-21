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

    public static CustomerResponse from(Customer customer, List<CustomerCouponResponse> coupons) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getMobileNumber(),
                customer.getPointBalance(),
                customer.getGrade(),
                coupons
        );
    }
}
