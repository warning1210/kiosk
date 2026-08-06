package com.kiosk.global.exception;

import com.kiosk.kiosk.payment.toss.TossPaymentException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// 서비스 계층에서 던지는 IllegalArgumentException/IllegalStateException을 이 핸들러가 없으면
// Spring이 메시지 없는 500으로 뭉개버려서, 프론트가 기대하는 { message }를 못 받는다.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        log.warn("잘못된 요청 처리 중 예외가 발생했습니다.", e);
        return ResponseEntity.badRequest().body(Map.of("message", "잘못된 요청입니다."));
    }

    // @Valid 검증에 걸리면 Spring 기본 응답은 필드 오류가 잔뜩 담긴 큰 JSON이라 화면에 그대로 못 쓴다.
    // 프론트가 항상 { message }만 읽으면 되도록, 첫 번째 오류 메시지만 꺼내 같은 형식으로 맞춘다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값을 확인해 주세요");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException e) {
        log.error("요청 상태 충돌 처리 중 예외가 발생했습니다.", e);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "요청을 처리할 수 없습니다."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
    }

    @ExceptionHandler(TossPaymentException.class)
    public ResponseEntity<Map<String, String>> handleTossPayment(TossPaymentException e) {

        log.error(
            "토스 결제 처리 중 예외가 발생했습니다. code={}",
            sanitizeForLog(e.getTossErrorCode()),
            e
        );

        return ResponseEntity.status(e.getHttpStatus())
                .body(Map.of("message", "결제 처리 중 오류가 발생했습니다."));
    }
    
    private static String sanitizeForLog(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", "")
                    .replace("\n", "");
    }
}
