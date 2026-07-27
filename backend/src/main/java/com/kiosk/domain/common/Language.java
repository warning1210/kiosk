package com.kiosk.domain.common;

/**
 * Enum constant names intentionally lowercase to match DB ENUM('ko','en','ja','zh') values,
 * since EnumType.STRING persists via name().
 */
public enum Language {
    ko,
    en,
    ja,
    zh
}
