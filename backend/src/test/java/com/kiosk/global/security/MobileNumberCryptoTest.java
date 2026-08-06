package com.kiosk.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MobileNumberCryptoTest {

    private final MobileNumberCrypto crypto = new MobileNumberCrypto(
            "test-hash-secret-32-bytes-minimum-length",
            "test-enc-secret-32-bytes-minimum-length!!"
    );

    @Test
    void hash_isDeterministicForSameInput() {
        assertThat(crypto.hash("01012345678")).isEqualTo(crypto.hash("01012345678"));
    }

    @Test
    void hash_differsForDifferentInput() {
        assertThat(crypto.hash("01012345678")).isNotEqualTo(crypto.hash("01099999999"));
    }

    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        String encrypted = crypto.encrypt("01012345678");

        assertThat(crypto.decrypt(encrypted)).isEqualTo("01012345678");
    }

    // GCM은 매번 랜덤 IV를 쓰므로, 같은 번호를 두 번 암호화해도 저장되는 암호문 자체는 달라야 한다 -
    // 그래야 DB가 유출돼도 같은 번호를 가진 고객들끼리 암호문만 보고 매칭이 안 된다.
    @Test
    void encrypt_producesDifferentCiphertextEachTimeButDecryptsToSameValue() {
        String first = crypto.encrypt("01012345678");
        String second = crypto.encrypt("01012345678");

        assertThat(first).isNotEqualTo(second);
        assertThat(crypto.decrypt(first)).isEqualTo(crypto.decrypt(second));
    }

    // GCM은 인증 태그가 같이 들어있어서, 다른 키로 복호화를 시도하면 값이 틀리게 나오는 게 아니라
    // 아예 예외가 터진다(위변조/키 불일치 탐지) - 그래서 "다른 값이 나옴"이 아니라 "실패함"을 검증한다.
    @Test
    void differentEncSecret_cannotDecryptEachOthersValue() {
        MobileNumberCrypto other = new MobileNumberCrypto(
                "test-hash-secret-32-bytes-minimum-length",
                "different-enc-secret-32-bytes-minimum-length"
        );
        String encrypted = crypto.encrypt("01012345678");

        assertThatThrownBy(() -> other.decrypt(encrypted)).isInstanceOf(IllegalStateException.class);
    }
}
