package com.kiosk.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 고객 전화번호를 평문으로 저장하지 않기 위한 해시 유틸리티.
 *
 * hash(): 조회 전용(WHERE 절 매칭). SHA-256이라 같은 입력이면 항상 같은 출력이 나오지만,
 * 해시에서 원래 번호를 복원할 수는 없다 - 원본이 필요한 화면(쿠폰 관리 등)은 복호화 대신
 * mask()로 만든 마스킹 값(예: 010****5678)을 별도 컬럼에 저장해 보여준다. 팀원끼리 맞춰야 하는
 * 비밀키가 없어서(작은 팀 프로젝트라 키 배포/동기화 부담을 감수할 만큼 필요하지 않다고 판단)
 * 설정값 없이 바로 동작한다.
 *
 * hash()/mask() 모두 숫자 11자리(010 + 8자리)가 아니면 IllegalArgumentException을 던진다 -
 * 이 앱이 다루는 전화번호는 형식이 고정이라, 형식이 다른 값이 조용히 이상한 해시/마스킹으로
 * 처리되는 대신 입력 단계에서 바로 걸러지게 한다.
 */
public class MobileNumberCrypto {

    // 이 프로젝트는 010 + 8자리(11자리) 번호만 받는다(키오스크 키패드가 PHONE_DIGITS=11로 강제).
    // API를 직접 호출하는 경우까지 대비해 백엔드 경계에서도 같은 전제를 검증한다.
    private static final Pattern MOBILE_NUMBER_PATTERN = Pattern.compile("^\\d{11}$");

    private MobileNumberCrypto() {
    }

    public static String hash(String plainMobileNumber) {
        requireValidFormat(plainMobileNumber);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(plainMobileNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("전화번호 해시 생성에 실패했습니다.", e);
        }
    }

    /** 앞 3자리·뒤 4자리만 남기고 가운데를 가린다 (010****5678). */
    public static String mask(String plainMobileNumber) {
        requireValidFormat(plainMobileNumber);
        return plainMobileNumber.substring(0, 3) + "****" + plainMobileNumber.substring(7);
    }

    private static void requireValidFormat(String mobileNumber) {
        if (mobileNumber == null || !MOBILE_NUMBER_PATTERN.matcher(mobileNumber).matches()) {
            throw new IllegalArgumentException("전화번호는 숫자 11자리여야 합니다.");
        }
    }
}
