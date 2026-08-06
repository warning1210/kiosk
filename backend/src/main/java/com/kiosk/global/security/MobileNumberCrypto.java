package com.kiosk.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 고객 전화번호를 평문으로 저장하지 않기 위한 해시/암복호화 유틸리티.
 *
 * - hash(): 조회 전용(WHERE 절 매칭). HMAC-SHA256이라 같은 입력이면 항상 같은 출력이 나오지만,
 *   해시에서 원래 번호를 복원할 수는 없다.
 * - encrypt()/decrypt(): 실제 번호가 필요할 때만 복원하는 저장용. AES-256-GCM이라 같은 입력이어도
 *   매번 다른 암호문이 나온다(IV 랜덤) - 그래서 이 값으로는 조회를 할 수 없고, hash()와 역할을 분리해서 쓴다.
 * 두 시크릿은 반드시 서로 달라야 한다 - 하나가 유출돼도 다른 쪽까지 같이 뚫리지 않도록.
 */
@Component
public class MobileNumberCrypto {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String AES_TRANSFORM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec hmacKey;
    private final SecretKeySpec aesKey;
    private final SecureRandom random = new SecureRandom();

    public MobileNumberCrypto(
            @Value("${mobile-number.hash-secret}") String hashSecret,
            @Value("${mobile-number.enc-secret}") String encSecret
    ) {
        requireStrongSecret("MOBILE_NUMBER_HASH_SECRET", hashSecret);
        requireStrongSecret("MOBILE_NUMBER_ENC_SECRET", encSecret);
        if (MessageDigest.isEqual(
                hashSecret.getBytes(StandardCharsets.UTF_8),
                encSecret.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalStateException(
                "MOBILE_NUMBER_HASH_SECRET과 MOBILE_NUMBER_ENC_SECRET은 서로 다른 값이어야 합니다."
            );
        }
        this.hmacKey = new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
        // AES는 키 길이가 정확히 16/24/32바이트여야 해서, 임의 길이 시크릿 문자열을 SHA-256으로 정규화해 32바이트(AES-256) 키로 쓴다.
        this.aesKey = new SecretKeySpec(sha256(encSecret), "AES");
    }

    private static void requireStrongSecret(String envVarName, String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(envVarName + "은 32바이트 이상의 값으로 설정해야 합니다.");
        }
    }

    public String hash(String plainMobileNumber) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(hmacKey);
            byte[] result = mac.doFinal(plainMobileNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("전화번호 해시 생성에 실패했습니다.", e);
        }
    }

    public String encrypt(String plainMobileNumber) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainMobileNumber.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("전화번호 암호화에 실패했습니다.", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("전화번호 복호화에 실패했습니다.", e);
        }
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("키 정규화에 실패했습니다.", e);
        }
    }
}
