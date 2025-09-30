package io.github.egorkor.webutils.queryparam.filterInternal;

import io.github.egorkor.webutils.exception.InvalidParameterException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum FilterOperation {
    EQUALS("="),
    NOT_EQUALS("!="),
    GT(">"),
    GTE(">="),
    LS("<"),
    LSE("<="),
    LIKE("like"),
    IS("is"),
    IS_NOT("is_not"),
    IN("in"),
    CONTAINS("contains"),
    NOT_CONTAINS("not_contains"),
    NOT_LIKE("not_like"),
    NOT_IN("not_in");


    private final String operation;

    public static FilterOperation parse(String operation){
        FilterOperation[] values = values();
        for(var filter: values){
            if(operation.equals(filter.getOperation())){
                return filter;
            }
        }
        throw new InvalidParameterException("Недопустимая операция: " + operation + ". Допустимые значения метода parse() - " +
                Arrays.stream(values).map(FilterOperation::getOperation).toList());
    }
}
