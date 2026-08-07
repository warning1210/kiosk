package com.kiosk.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MobileNumberCryptoTest {

    @Test
    void hash_isDeterministicForSameInput() {
        assertThat(MobileNumberCrypto.hash("01012345678")).isEqualTo(MobileNumberCrypto.hash("01012345678"));
    }

    @Test
    void hash_differsForDifferentInput() {
        assertThat(MobileNumberCrypto.hash("01012345678")).isNotEqualTo(MobileNumberCrypto.hash("01099999999"));
    }

    @Test
    void hash_withWrongLength_throwsIllegalArgument() {
        assertThatThrownBy(() -> MobileNumberCrypto.hash("123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mask_keepsFirstThreeAndLastFourDigits() {
        assertThat(MobileNumberCrypto.mask("01012345678")).isEqualTo("010****5678");
    }

    @Test
    void mask_withWrongLength_throwsIllegalArgument() {
        assertThatThrownBy(() -> MobileNumberCrypto.mask("1234567"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mask_withNonDigits_throwsIllegalArgument() {
        assertThatThrownBy(() -> MobileNumberCrypto.mask("010-1234-5678"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
