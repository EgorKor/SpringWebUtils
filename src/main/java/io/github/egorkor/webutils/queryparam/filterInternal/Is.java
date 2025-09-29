package io.github.egorkor.webutils.queryparam.filterInternal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Is {
    TRUE("true"),
    FALSE("false"),
    NULL("null"),
    NOT_NULL("not_null");

    private final String value;
}
