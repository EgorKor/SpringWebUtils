package io.github.egorkor.webutils.queryparam.filterInternal;

import io.github.egorkor.webutils.exception.InvalidParameterException;
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

    public static Is parse(String string) {
        string = string.toLowerCase();
        return switch (string){
            case "true" -> TRUE;
            case "false" -> FALSE;
            case "null" -> NULL;
            case "not_null" -> NOT_NULL;
            default -> throw new InvalidParameterException("Некорректное значение операции is: " + string + ". Допустимы значения ['true','false','null','not_null']");
        };
    }
}
