package com.kiosk.kiosk.payment.toss;
 
import org.springframework.http.HttpStatus;
 
public class TossPaymentException extends RuntimeException {
 
    private final HttpStatus httpStatus;
    private final String tossErrorCode;
 
    public TossPaymentException(HttpStatus httpStatus, String tossErrorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.tossErrorCode = tossErrorCode;
    }
 
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
 
    public String getTossErrorCode() {
        return tossErrorCode;
    }
}
 