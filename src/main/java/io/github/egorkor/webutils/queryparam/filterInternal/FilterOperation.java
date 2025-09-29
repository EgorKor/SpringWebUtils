package io.github.egorkor.webutils.queryparam.filterInternal;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
    IN("in"),
    CONTAINS("contains"),
    NOT_CONTAINS("not_contains"),
    NOT_LIKE("not_like"),
    NOT_IN("not_in");


    private final String operation;

    public static FilterOperation parse(String operation){
        for(var filter: values()){
            if(operation.equals(filter.getOperation())){
                return filter;
            }
        }
        throw new IllegalArgumentException("Invalid operation: " + operation);
    }
}
