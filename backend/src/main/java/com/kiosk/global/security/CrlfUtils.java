package com.kiosk.global.security;

/**
 * CRLF(줄바꿈) 로그 위조 방지 유틸리티.
 *
 * <p>사용자 입력이나 외부 시스템 응답을 로그로 남기기 전에 개행 문자(CR {@code \r}, LF {@code \n})를
 * 제거한다. 개행이 그대로 로그에 들어가면 공격자가 "가짜 로그 줄"을 삽입(로그 위조, CWE-117)할 수 있다.
 *
 * <p>사용 예:
 * <pre>{@code
 * log.warn("로그인 실패: id={}", CrlfUtils.forLog(loginId));
 * }</pre>
 */
public final class CrlfUtils {

    private CrlfUtils() {
        // 유틸리티 클래스 - 인스턴스화 금지
    }

    /**
     * 로그로 남기기 전에 개행 문자(\r, \n)를 제거한다. null 안전.
     *
     * @param value 신뢰할 수 없는 원본 문자열(로그 대상)
     * @return 개행이 제거된 안전한 문자열, 입력이 {@code null} 이면 {@code null}
     */
    public static String forLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", "").replace("\n", "");
    }
}
