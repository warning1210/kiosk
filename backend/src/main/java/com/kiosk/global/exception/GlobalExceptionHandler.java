package com.kiosk.global.exception;

import com.kiosk.kiosk.payment.toss.TossPaymentException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// 서비스 계층에서 던지는 IllegalArgumentException/IllegalStateException을 이 핸들러가 없으면
// Spring이 메시지 없는 500으로 뭉개버려서, 프론트가 기대하는 { message }를 못 받는다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
    }

    @ExceptionHandler(TossPaymentException.class)
    public ResponseEntity<Map<String, String>> handleTossPayment(TossPaymentException e) {
        return ResponseEntity.status(e.getHttpStatus()).body(Map.of("message", e.getMessage()));
    }
}
