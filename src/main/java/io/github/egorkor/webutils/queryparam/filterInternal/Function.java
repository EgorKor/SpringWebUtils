package io.github.egorkor.webutils.queryparam.filterInternal;

import io.github.egorkor.webutils.exception.InvalidParameterException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Function {
    LENGTH("length()"),
    SIZE("size()");


    private final String function;

    public static Function parseByOperation(String operation) {
        for (Function func : values()) {
            if (operation.equals(func.function)) {
                return func;
            }
        }
        throw new InvalidParameterException("Недопустимая функция: " + operation);
    }
}
