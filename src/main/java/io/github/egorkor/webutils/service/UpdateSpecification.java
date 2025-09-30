package io.github.egorkor.webutils.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateSpecification {
    private Map<String, UpdateUnit> updates = new HashMap<>();
    public static UpdateSpecificationBuilder ub = new UpdateSpecificationBuilder();

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


    public static UpdateSpecification updateValue(String field, Object value) {
        return ub.update(ub.updateValue(field, value));
    }

    public static UpdateSpecification setNull(String field) {
        return ub.update(ub.setNull(field));
    }

    public static UpdateSpecification plus(String field, Byte value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Short value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Integer value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Long value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Float value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification plus(String field, Double value) {
        return ub.update(ub.plus(field, value));
    }

    public static UpdateSpecification minus(String field, Byte value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Short value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Integer value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Long value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Float value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification minus(String field, Double value) {
        return ub.update(ub.minus(field, value));
    }

    public static UpdateSpecification concat(String field, String value) {
        return ub.update(ub.concat(field, value));
    }

    public static UpdateSpecification truncateTime(String field) {
        return ub.update(ub.truncateTime(field));
    }

    public static UpdateSpecification addDays(String field, Integer days) {
        return ub.update(ub.addDays(field, days));
    }

    public static UpdateSpecification toUpperCase(String field) {
        return ub.update(ub.toUpperCase(field));
    }

    public static UpdateSpecification toLowerCase(String field) {
        return ub.update(ub.toLowerCase(field));
    }

    public static UpdateSpecification copyValue(String from, String to) {
        return ub.update(ub.copyValue(from, to));
    }


    public record UpdateUnit(String field, Action action, Object data) {
    }


    public static class UpdateSpecificationBuilder {

        public UpdateUnit setNull(String field) {
            return new UpdateUnit(field, Action.UPDATE, null);
        }

        public UpdateUnit updateValue(String field, Object value) {
            return new UpdateUnit(field, Action.UPDATE, value);
        }

        public UpdateUnit multiply(String field, Number value) {
            return new UpdateUnit(field, Action.MULTIPLY, value);
        }

        public UpdateUnit divide(String field, Number value) {
            return new UpdateUnit(field, Action.DIVIDE, value);
        }

        public UpdateUnit increment(String field) {
            return plus(field, 1);
        }

        public UpdateUnit decrement(String field) {
            return minus(field, 1);
        }

        public UpdateUnit copyValue(String fromField, String toField) {
            return new UpdateUnit(toField, Action.COPY, fromField);
        }

        public UpdateUnit plus(String field, Number value) {
            return new UpdateUnit(field, Action.SUM, value);
        }

        public UpdateUnit minus(String field, Float value) {
            return new UpdateUnit(field, Action.SUM, -value);
        }

        public UpdateUnit minus(String field, Double value) {
            return new UpdateUnit(field, Action.SUM, -value);
        }

        public UpdateUnit minus(String field, Byte value) {
            return new UpdateUnit(field, Action.SUM, -value);
        }

        public UpdateUnit minus(String field, Short value) {
            return new UpdateUnit(field, Action.SUM, -value);
        }

        public UpdateUnit minus(String field, Integer value) {
            return new UpdateUnit(field, Action.SUM, -value);
        }

        public UpdateUnit minus(String field, Long value) {
            return new UpdateUnit(field, Action.SUM, -value);
        }

        public UpdateUnit addDays(String field, int days) {
            return new UpdateUnit(field, Action.ADD_DAYS, days);
        }

        public UpdateUnit truncateTime(String field) {
            return new UpdateUnit(field, Action.TRUNCATE_TIME, null);
        }

        public UpdateUnit concat(String field, String value) {
            return new UpdateUnit(field, Action.CONCAT, value);
        }

        public UpdateUnit toUpperCase(String field) {
            return new UpdateUnit(field, Action.UPPER_CASE, null);
        }

        public UpdateUnit toLowerCase(String field) {
            return new UpdateUnit(field, Action.LOWER_CASE, null);
        }

        public UpdateSpecification update(UpdateUnit... updates) {
            Map<String, UpdateUnit> updateMap = Arrays.stream(updates).collect(Collectors.toMap(
                    UpdateUnit::field, o -> o
            ));
            return new UpdateSpecification(updateMap);
        }
    }

}
