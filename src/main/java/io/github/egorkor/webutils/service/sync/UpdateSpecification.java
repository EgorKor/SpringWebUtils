package io.github.egorkor.webutils.service.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateSpecification {
    private Map<String, UpdatePair> updates = new HashMap<>();


    public record UpdatePair(Action action, Object data) {
    }

    public enum Action {
        UPDATE,
        SUM,
        MULTIPLY,
        DIVIDE,
        ADD_DAYS,
        TRUNCATE_TIME,
        CONCAT,
        UPPER_CASE,
        LOWER_CASE,
        COPY
    }

    public static class UpdateSpecificationBuilder {
        private final Map<String, UpdatePair> updates = new HashMap<>();

        public UpdateSpecificationBuilder setNull(String field) {
            updates.put(field, new UpdatePair(Action.UPDATE, null));
            return this;
        }

        public UpdateSpecificationBuilder updateValue(String field, Object value) {
            updates.put(field, new UpdatePair(Action.UPDATE, value));
            return this;
        }

        public UpdateSpecificationBuilder multiply(String field, Number value) {
            updates.put(field, new UpdatePair(Action.MULTIPLY, value));
            return this;
        }

        public UpdateSpecificationBuilder divide(String field, Number value) {
            updates.put(field, new UpdatePair(Action.DIVIDE, value));
            return this;
        }

        public UpdateSpecificationBuilder increment(String field) {
            return plus(field, 1);
        }

        public UpdateSpecificationBuilder decrement(String field) {
            return minus(field, 1);
        }

        public UpdateSpecificationBuilder copyValue(String fromField, String toField) {
            updates.put(toField, new UpdatePair(Action.COPY, fromField));
            return this;
        }

        public UpdateSpecificationBuilder plus(String field, Number value) {
            updates.put(field, new UpdatePair(Action.SUM, value));
            return this;
        }

        public UpdateSpecificationBuilder minus(String field, Float value) {
            updates.put(field, new UpdatePair(Action.SUM, -value));
            return this;
        }

        public UpdateSpecificationBuilder minus(String field, Double value) {
            updates.put(field, new UpdatePair(Action.SUM, -value));
            return this;
        }

        public UpdateSpecificationBuilder minus(String field, Byte value) {
            updates.put(field, new UpdatePair(Action.SUM, -value));
            return this;
        }

        public UpdateSpecificationBuilder minus(String field, Short value) {
            updates.put(field, new UpdatePair(Action.SUM, -value));
            return this;
        }

        public UpdateSpecificationBuilder minus(String field, Integer value) {
            updates.put(field, new UpdatePair(Action.SUM, -value));
            return this;
        }

        public UpdateSpecificationBuilder minus(String field, Long value) {
            updates.put(field, new UpdatePair(Action.SUM, -value));
            return this;
        }

        public UpdateSpecificationBuilder addDays(String field, int days) {
            updates.put(field, new UpdatePair(Action.ADD_DAYS, days));
            return this;
        }

        public UpdateSpecificationBuilder truncateTime(String field) {
            updates.put(field, new UpdatePair(Action.TRUNCATE_TIME, null));
            return this;
        }

        public UpdateSpecificationBuilder concat(String field, String value) {
            updates.put(field, new UpdatePair(Action.CONCAT, value));
            return this;
        }

        public UpdateSpecificationBuilder upperCase(String field) {
            updates.put(field, new UpdatePair(Action.UPPER_CASE, null));
            return this;
        }

        public UpdateSpecificationBuilder lowerCase(String field) {
            updates.put(field, new UpdatePair(Action.LOWER_CASE, null));
            return this;
        }

        public UpdateSpecification build() {
            return new UpdateSpecification(updates);
        }
    }

}
