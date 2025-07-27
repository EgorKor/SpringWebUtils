package io.github.egorkor.webutils.analyze.jpa;

import lombok.Getter;

import java.util.Map;

@Getter
public enum ValidatorCode {
    NOT_NULL("not_null", null),
    NOT_EMPTY("not_empty", null),
    NOT_BLANK("not_blank", null),
    RANGE("range",  Map.of("min", Integer.MIN_VALUE, "max", Integer.MAX_VALUE)),
    SIZE("size", Map.of("min", 0, "max", Integer.MAX_VALUE)),
    MIN("min_value", Map.of("value", 0L)),
    MAX("max_value", Map.of("value", Long.MAX_VALUE)),
    PATTERN("regex", Map.of("value", "")),
    EMAIL("email", null),
    POSITIVE("positive", null),
    POSITIVE_OR_ZERO("positive_or_zero", null),
    NEGATIVE("negative", null),
    NEGATIVE_OR_ZERO("negative_or_zero", null),
    FUTURE("future", null),
    FUTURE_OR_PRESENT("future_or_present", null),
    PAST("past", null),
    PAST_OR_PRESENT("past_or_present", null),
    DIGITS("digits", Map.of("integer", 0, "fraction", 0)),
    DECIMAL_MIN("decimal_min", Map.of("value", "0.0")),
    DECIMAL_MAX("decimal_max", Map.of("value", "0.0"));

    // Геттеры
    private final String code;
    private final Map<String, Object> defaultParams;

    ValidatorCode(String code, Map<String, Object> defaultParams) {
        this.code = code;
        this.defaultParams = defaultParams;
    }

}
