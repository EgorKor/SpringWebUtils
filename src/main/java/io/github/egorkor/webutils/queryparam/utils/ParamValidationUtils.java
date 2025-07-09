package io.github.egorkor.webutils.queryparam.utils;

import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountLimit;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParamValidationUtils {

    private static final HashMap<ParamType, BiFunction<Integer, Integer, String>> LIMIT_ERRORS = new HashMap<>();

    static {
        LIMIT_ERRORS.put(ParamType.SORT,
                (size, value) -> "Недопустимое кол-во параметров сортировки: "
                        + size
                        + " , допустимое кол-во: "
                        + value);
        LIMIT_ERRORS.put(ParamType.FILTER,
                (size, value) -> "Недопустимое кол-во параметров фильтрации: "
                        + size
                        + " , допустимое кол-во: "
                        + value);
    }

    private static final HashMap<ParamType, Function<Set, String>> NON_ALLOWED_ERRORS = new HashMap<>();

    static {
        NON_ALLOWED_ERRORS.put(ParamType.SORT, (set) ->
                "Недопустимые параметры сортировки: " + set);
        NON_ALLOWED_ERRORS.put(ParamType.FILTER, (set) ->
                "Недопустимые параметры фильтрации: " + set);
    }


    public enum ParamType {
        SORT, FILTER
    }

    public static void validateAllowedParams(List<String> params,
                                             Class<?> paramsClass,
                                             ParamType paramType,
                                             Function<String, String[]> validationFunc,
                                             List<String> whiteList) {
        ParamCountLimit limit;
        if ((limit = paramsClass.getAnnotation(ParamCountLimit.class)) != null
                && limit.value() != ParamCountLimit.UNLIMITED
                && params.size() > limit.value()) {
            throw new IllegalArgumentException(LIMIT_ERRORS.get(paramType).apply(params.size(), params.size()));
        }

        Set<String> paramsNames = params.stream().map(
                s -> validationFunc.apply(s)[0]
        ).collect(Collectors.toSet());

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
            throw new IllegalArgumentException(NON_ALLOWED_ERRORS.get(paramType).apply(paramsNames));
        }
    }


    public static void mapParamsByFilter(
            List<String> params,
            Class<?> paramsClass,
            Function<String, String[]> validationFunc) {
        Field[] fields = paramsClass.getDeclaredFields();
        for (Field field : fields) {
            FieldParamMapping fieldParamMapping = field.getAnnotation(FieldParamMapping.class);
            if (fieldParamMapping == null
                    || fieldParamMapping.sqlMapping().equals(FieldParamMapping.NO_MAPPING)) {
                continue;
            }
            String alliesName = fieldParamMapping.sqlMapping();
            String fieldName = Objects.equals(fieldParamMapping.requestParamMapping(), FieldParamMapping.NO_MAPPING) ? field.getName() : fieldParamMapping.requestParamMapping();
            String regexSafeFieldName = Pattern.quote(fieldName);
            for (int i = 0; i < params.size(); i++) {
                String filterFieldName = validationFunc.apply(params.get(i))[0];
                if (fieldName.equals(filterFieldName)) {
                    params.set(i, params.get(i)
                            .replaceFirst(regexSafeFieldName, alliesName));
                    break;
                }
            }
        }
    }
}
