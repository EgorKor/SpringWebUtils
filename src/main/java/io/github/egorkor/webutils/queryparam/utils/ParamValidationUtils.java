package io.github.egorkor.webutils.queryparam.utils;

import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountLimit;
import io.github.egorkor.webutils.exception.InvalidParameterException;
import io.github.egorkor.webutils.queryparam.filterInternal.FilterBasicOperation;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParamValidationUtils {

    private static final HashMap<ParamType, BiFunction<Integer, Integer, String>> LIMIT_ERRORS = new HashMap<>(2);
    private static final HashMap<ParamType, Function<Set, String>> NON_ALLOWED_ERRORS = new HashMap<>(2);

    static {
        LIMIT_ERRORS.put(ParamType.SORT,
                (size, value) -> "Illegal sort param count: "
                        + size
                        + " , allowed count: "
                        + value);
        LIMIT_ERRORS.put(ParamType.FILTER,
                (size, value) -> "Illegal filter param count: "
                        + size
                        + " , allowed count: "
                        + value);
    }

    static {
        NON_ALLOWED_ERRORS.put(ParamType.SORT, (set) ->
                "Illegal sort params: " + set);
        NON_ALLOWED_ERRORS.put(ParamType.FILTER, (set) ->
                "Illegal filter params: " + set);
    }

    public static void validateAllowedParams(List<Object> params,
                                             Class<?> paramsClass,
                                             ParamType paramType,
                                             Function<Object, String> paramNameExtractor,
                                             List<String> whiteList) {
        ParamCountLimit limit;
        if ((limit = paramsClass.getAnnotation(ParamCountLimit.class)) != null
                && limit.value() != ParamCountLimit.UNLIMITED
                && params.size() > limit.value()) {
            throw new InvalidParameterException(LIMIT_ERRORS.get(paramType).apply(params.size(), params.size()));
        }

        Set<String> paramsNames = params.stream()
                .map(paramNameExtractor)
                .collect(Collectors.toSet());

        Set<String> allowedFields = Arrays.stream(paramsClass.getDeclaredFields())
                .map(f -> {
                    FieldParamMapping allies;
                    if ((allies = f.getAnnotation(FieldParamMapping.class)) != null
                            && !Objects.equals(allies.requestParamMapping(), FieldParamMapping.NO_MAPPING)) {
                        return allies.requestParamMapping();
                    } else {
                        return f.getName();
                    }
                })
                .collect(Collectors.toSet());

        paramsNames.removeAll(allowedFields);
        whiteList.forEach(paramsNames::remove);
        if (!paramsNames.isEmpty()) {
            throw new InvalidParameterException(NON_ALLOWED_ERRORS.get(paramType).apply(paramsNames));
        }
    }

    public static void mapParamsByFilter(
            List<FilterBasicOperation> params,
            Class<?> paramsClass) {
        Field[] fields = paramsClass.getDeclaredFields();
        for (Field field : fields) {
            FieldParamMapping fieldParamMapping = field.getAnnotation(FieldParamMapping.class);
            if (fieldParamMapping == null
                    || fieldParamMapping.sqlMapping().equals(FieldParamMapping.NO_MAPPING)) {
                continue;
            }
            String alliesName = fieldParamMapping.sqlMapping();
            String fieldName = Objects.equals(fieldParamMapping.requestParamMapping(), FieldParamMapping.NO_MAPPING)
                    ? field.getName() : fieldParamMapping.requestParamMapping();
            String regexSafeFieldName = Pattern.quote(fieldName);

            for (int i = 0; i < params.size(); i++) {
                FilterBasicOperation op = params.get(i);

                if (fieldName.equals(op.field())) {
                    params.set(i, new FilterBasicOperation(
                            op.field().replaceFirst(regexSafeFieldName, alliesName),
                            op.operation(),
                            op.value()));
                }

            }
        }
    }


    public enum ParamType {
        SORT, FILTER
    }
}
