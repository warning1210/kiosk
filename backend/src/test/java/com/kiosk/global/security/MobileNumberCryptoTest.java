package com.kiosk.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MobileNumberCryptoTest {

    private final MobileNumberCrypto crypto = new MobileNumberCrypto(
            "test-hash-secret-32-bytes-minimum-length"
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
    void hash_withBlankSecret_throwsIllegalState() {
        assertThatThrownBy(() -> new MobileNumberCrypto(""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mask_keepsFirstThreeAndLastFourDigits() {
        assertThat(MobileNumberCrypto.mask("01012345678")).isEqualTo("010****5678");
    }

    @Test
    void mask_shortInput_masksEverything() {
        assertThat(MobileNumberCrypto.mask("1234567")).isEqualTo("*******");
    }
}
