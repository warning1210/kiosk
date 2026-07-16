package com.kiosk.kiosk.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/{mobileNumber}")
    public CustomerResponse getByMobileNumber(@PathVariable String mobileNumber) {
        return customerService.getByMobileNumber(mobileNumber);
    }
}
