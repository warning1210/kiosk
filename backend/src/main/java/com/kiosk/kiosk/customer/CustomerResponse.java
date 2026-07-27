package com.kiosk.kiosk.customer;

import com.kiosk.domain.customer.Customer;
import com.kiosk.domain.customer.CustomerGrade;

public record CustomerResponse(Long customerId, String mobileNumber, Integer pointBalance, CustomerGrade grade) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getMobileNumber(),
                customer.getPointBalance(),
                customer.getGrade()
        );
    }
}
