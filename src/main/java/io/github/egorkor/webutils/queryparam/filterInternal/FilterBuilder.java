package io.github.egorkor.webutils.queryparam.filterInternal;

import io.github.egorkor.webutils.queryparam.Filter;

import java.util.Arrays;
import java.util.Collection;

import static io.github.egorkor.webutils.queryparam.filterInternal.FilterOperation.*;

public class FilterBuilder {

    public FilterBasicOperation equals(String field, Object value) {
        return new FilterBasicOperation(field, EQUALS, value);
    }

    public FilterBasicOperation notEquals(String field, Object value) {
        return new FilterBasicOperation(field, NOT_EQUALS, value);
    }

    public FilterBasicOperation less(String field, Comparable<?> value) {
        return new FilterBasicOperation(field, LS, value);
    }

    public FilterBasicOperation lessOrEquals(String field, Comparable<?> value) {
        return new FilterBasicOperation(field, LSE, value);
    }

    public FilterBasicOperation greater(String field, Comparable<?> value) {
        return new FilterBasicOperation(field, GT, value);
    }

    public FilterBasicOperation greaterOrEquals(String field, Comparable<?> value) {
        return new FilterBasicOperation(field, GTE, value);
    }

    public FilterBasicOperation like(String field, String value) {
        return new FilterBasicOperation(field, LIKE, value);
    }

    public FilterBasicOperation contains(String field, String value) {
        return new FilterBasicOperation(field, CONTAINS, value);
    }

    public FilterBasicOperation notContains(String field, String value) {
        return new FilterBasicOperation(field, NOT_CONTAINS, value);
    }

    public FilterBasicOperation in(String field, Object... values) {
        return new FilterBasicOperation(field, IN, String.join(";", Arrays.stream(values).map(Object::toString).toList()));
    }

    public FilterBasicOperation in(String field, Collection<Object> values) {
        return new FilterBasicOperation(field, IN, String.join(";", values.stream().map(Object::toString).toList()));
    }

    public FilterBasicOperation is(String field, Is value) {
        return new FilterBasicOperation(field, IS, value.getValue());
    }

    public FilterBasicOperation notLike(String field, String value) {
        return new FilterBasicOperation(field, NOT_LIKE, value);
    }

    public FilterBasicOperation notIn(String field, Object... values) {
        return new FilterBasicOperation(field, NOT_IN, String.join(";", Arrays.stream(values).map(Object::toString).toList()));
    }

    public Filter buildAnd(FilterBasicOperation... units) {
        return new Filter(Arrays.asList(units));
    }

}
