package io.github.egorkor.webutils.queryparam.filterInternal;

import lombok.ToString;


public record FilterBasicOperation(String field, FilterOperation operation, Object value) {

    @Override
    public String toString() {
        return "Filter('%s' %s %s)".formatted(field, operation.getOperation(), value);
    }

}
