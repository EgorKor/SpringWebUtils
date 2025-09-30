package io.github.egorkor.webutils.queryparam;

import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountLimit;
import io.github.egorkor.webutils.exception.InvalidParameterException;
import io.github.egorkor.webutils.queryparam.sortingInternal.SortingBuilder;
import io.github.egorkor.webutils.queryparam.sortingInternal.SortingUnit;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.github.egorkor.webutils.queryparam.Filter.getNestedPath;

/**
 * Параметр запроса для сортировки запрашиваемых ресурсов.
 * Пример использования в контроллере:
 * <pre>{@code
 * public void controllerMethod(@RequestParam SortParams sort)
 * }</pre>
 * Пример использования с JPA репозиториями
 * <pre>
 * {@code
 * public List<Entity> query(SortParams sort){
 *     repository.findAll(sort.toJpaSort());
 * }
 * }
 * </pre>
 *
 * @author EgorKor
 * @since 2025
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Sorting {
    public static SortingBuilder sb = new SortingBuilder();
    public static final String DESC = "desc";
    public static final String ASC = "asc";
    private List<SortingUnit> sort = new ArrayList<>();

    public static Sorting unsorted() {
        return new Sorting();
    }

    public static SortingBuilder builder() {
        return new SortingBuilder();
    }

    public void checkAllowedSortFields() {
        if (isMethodCallByParentClass()) {
            return;
        }
        //TODO починить
        ParamCountLimit limit;
        if ((limit = this.getClass().getAnnotation(ParamCountLimit.class)) != null
                && limit.value() != ParamCountLimit.UNLIMITED
                && sort.size() > limit.value()) {
            throw new InvalidParameterException("Недопустимое кол-во параметров сортировки: " + sort.size());
        }

        Set<String> paramsNames = sort.stream()
                .map(SortingUnit::field)
                .collect(Collectors.toSet());

        Set<String> allowedFields = Arrays.stream(this.getClass().getDeclaredFields())
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
        //whiteList.forEach(paramsNames::remove);
        if (!paramsNames.isEmpty()) {
            throw new InvalidParameterException("Недопустимые параметры сортировки: " + paramsNames);
        }
    }

    private <T extends Sorting> T _this() {
        return (T) this;
    }

    public <T extends Sorting> T withDefault(String field, String order) {
        if (isUnsorted()) {
            sort.add(new SortingUnit(field, order));
        }
        return _this();
    }

    public <T extends Sorting> T withDefaultAsc(String field) {
        return withDefault(field, ASC);
    }

    public <T extends Sorting> T withDefaultDesc(String field) {
        return withDefault(field, DESC);
    }

    public void mapSortByAllies() {
        if (isMethodCallByParentClass()) {
            return;
        }
        //TODO починить
        Field[] fields = this.getClass().getDeclaredFields();
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

            for (int i = 0; i < sort.size(); i++) {
                SortingUnit op = sort.get(i);

                if (fieldName.equals(op.field())) {
                    sort.set(i, new SortingUnit(
                            op.field().replaceFirst(regexSafeFieldName, alliesName),
                            op.order()));
                }

            }
        }
    }

    private boolean isMethodCallByParentClass() {
        return this.getClass() == Sorting.class;
    }

    public boolean isSorted() {
        return !sort.isEmpty();
    }

    public boolean isUnsorted() {
        return sort.isEmpty();
    }

    public <T> List<Order> toCriteriaOrderList(@NonNull Root<T> root,
                                               @NonNull CriteriaBuilder cb) {
        List<Order> orderList = new ArrayList<>();
        for (SortingUnit s : sort) {
            Path<T> path = s.field().contains(".") ? getNestedPath(root, s.field()) : root.get(s.field());
            if (s.order().equalsIgnoreCase("asc")) {
                orderList.add(cb.asc(path));
            } else if (s.order().equalsIgnoreCase("desc")) {
                orderList.add(cb.desc(path));
            }
        }
        return orderList;
    }

    public Sort toJpaSort() {
        if (sort.isEmpty()) {
            return Sort.unsorted();
        }
        checkAllowedSortFields();
        List<Sort.Order> orders = sort.stream()
                .map(param -> new Sort.Order(
                        Sort.Direction.fromString(param.order().toLowerCase()),
                        param.field()
                ))
                .toList();
        return Sort.by(orders);
    }

    public static Sorting asc(String field) {
        return new Sorting(List.of(new SortingUnit(field, "asc")));
    }

    public static Sorting desc(String field) {
        return new Sorting(List.of(new SortingUnit(field, "desc")));
    }


}
