package io.github.egorkor.webutils.queryparam;

import io.github.egorkor.webutils.queryparam.utils.ParamValidationUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

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
//TODO: group by
@Data
public class Sorting {
    private List<String> sort = new ArrayList<>();

    public Sorting() {
    }

    public Sorting(List<String> sort) {
        this.sort = sort;
    }

    public void checkAllowedSortFields() {
        if (isMethodCallByParentClass()) {
            return;
        }
        ParamValidationUtils.validateAllowedParams(sort, this.getClass(),
                ParamValidationUtils.ParamType.SORT, this::validateAndSplitSort,
                List.of());
    }

    public void mapSortByAllies() {
        if (isMethodCallByParentClass()) {
            return;
        }
        ParamValidationUtils.mapParamsByFilter(sort, this.getClass(), this::validateAndSplitSort);
    }

    private boolean isMethodCallByParentClass() {
        return this.getClass() == Sorting.class;
    }

    public static Sorting unsorted() {
        return new Sorting();
    }

    public boolean isSorted() {
        return !sort.isEmpty();
    }

    public boolean isUnsorted() {
        return sort.isEmpty();
    }

    public String toSQLSort() {
        return toSQLSort("");
    }

    public String toSQLSort(String prefix) {
        checkAllowedSortFields();
        StringBuilder sb = new StringBuilder();
        for (String s : sort) {
            if (sb.isEmpty()) {
                sb.append("ORDER BY ");
            }
            String[] sortParts = validateAndSplitSort(s);
            String field = sortParts[0];
            String order = sortParts[1];
            sb.append(prefix)
                    .append(field)
                    .append(" ")
                    .append(order.toUpperCase())
                    .append(", ");
        }
        if (!sb.isEmpty()) {
            sb.deleteCharAt(sb.length() - 2);
        }
        return sb.toString();
    }

    public <T> List<Order> toCriteriaOrderList(Root<T> root, CriteriaBuilder cb) {
        checkAllowedSortFields();
        List<Order> orderList = new ArrayList<>();
        for (String s : sort) {
            String[] sortParts = validateAndSplitSort(s);
            String field = sortParts[0];
            String order = sortParts[1];
            Path<T> path = field.contains(".") ? getNestedPath(root, field) : root.get(field);
            if (order.equalsIgnoreCase("asc")) {
                orderList.add(cb.asc(path));
            } else if (order.equalsIgnoreCase("desc")) {
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
                .map(this::validateAndSplitSort)
                .map(param -> new Sort.Order(
                        Sort.Direction.fromString(param[1].toLowerCase()),
                        param[0].toLowerCase()
                ))
                .toList();
        return Sort.by(orders);
    }

    private String[] validateAndSplitSort(String sort) {
        String[] split = sort.split(":");
        if (split.length != 2) {
            throw new IllegalArgumentException("Параметр сортировки должен иметь строго две части " +
                    "по шаблону 'field:order': " + sort);
        }
        validateField(split[0]);
        validateOrder(split[1]);
        return split;
    }

    private void validateField(String field) {
        if (!field.matches("[._\\-a-zA-Z]+")) {
            throw new IllegalArgumentException("Невалидное название поля параметра сортировки: " + field);
        }
    }

    private void validateOrder(String order) {
        if (!(order.equals("asc") || order.equals("desc"))) {
            throw new IllegalArgumentException("Невалидный порядок сортировки: " + order +
                    ". Допустимые порядки - 'asc' 'desc'");
        }
    }

    public static SortingBuilder builder() {
        return new SortingBuilder();
    }

    public static class SortingBuilder {
        private final List<SortingUnit> sorting = new ArrayList<>();

        public SortingBuilder asc(String field) {
            sorting.add(new SortingUnit(field, "asc"));
            return this;
        }

        public SortingBuilder desc(String field) {
            sorting.add(new SortingUnit(field, "desc"));
            return this;
        }

        public Sorting build() {
            return new Sorting(new ArrayList<>(sorting.stream()
                    .map(s -> "%s:%s".formatted(s.field(), s.order())).toList()));
        }

        @SneakyThrows
        public <R extends Sorting> R buildDerived(Class<R> derivedClass) {
            R derivedSort = derivedClass.getDeclaredConstructor().newInstance();
            derivedSort.setSort(
                    new ArrayList<>(sorting.stream().map(
                            (o) -> "%s:%s".formatted(o.field(),o.order())
                    ).toList()
                    ));
            return derivedSort;
        }
    }

    public record SortingUnit(String field, String order) {
    }

}
