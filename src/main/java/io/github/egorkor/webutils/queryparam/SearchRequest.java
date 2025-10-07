package io.github.egorkor.webutils.queryparam;

import io.github.egorkor.webutils.queryparam.filterInternal.FilterBasicOperation;
import io.github.egorkor.webutils.queryparam.filterInternal.FilterOperation;
import io.github.egorkor.webutils.queryparam.sortingInternal.SortingUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.egorkor.webutils.queryparam.Sorting.ASC;

@AllArgsConstructor
public class SearchRequest {

    private static final Set<String> NON_FILTER_KEYS;
    public final static String SORT_PARAM;
    public final static String PAGE_PARAM;
    public final static String PAGE_SIZE_PARAM;
    private final static Pattern FILTER_PATTERN;
    private final static Map<String, String> FILTER_PREFIX_MAPPING;

    static {
        FILTER_PREFIX_MAPPING = new HashMap<>();
        FILTER_PATTERN =
                Pattern.compile("(contains|not_contains|like|not_like|gt|lt|ge|le|in|not_in|not_equals|is|is_not):(.*)");
        SORT_PARAM = "sort";
        PAGE_PARAM = "page";
        PAGE_SIZE_PARAM = "pageSize";
        FILTER_PREFIX_MAPPING.put("gt", ">");
        FILTER_PREFIX_MAPPING.put("ge", ">=");
        FILTER_PREFIX_MAPPING.put("lt", "<");
        FILTER_PREFIX_MAPPING.put("le", "<=");
        FILTER_PREFIX_MAPPING.put("not_equals", "!=");
        NON_FILTER_KEYS = Set.of(SORT_PARAM, PAGE_SIZE_PARAM, PAGE_PARAM);
    }

    @Getter
    private final Pagination pagination;
    private final Filter filter;
    private final Sorting sorting;

    public SearchRequest(MultiValueMap<String, String> params) {
        this(params, Filter.class, Sorting.class);
    }

    public <F extends Filter<?>, S extends Sorting> SearchRequest(MultiValueMap<String, String> params,
                  Class<F> filterClass, Class<S> sortingClass) {
        if (filterClass == null) {
            Class<? extends Filter> fClass = Filter.class;
            filterClass = (Class<F>) fClass;
        }
        if (sortingClass == null) {
            sortingClass = (Class<S>) Sorting.class;
        }
        this.pagination = parsePagination(params);
        this.filter = parseFilter(params, filterClass);
        this.filter.checkAllowedFilterFields();
        this.filter.checkAllowedFilterOperations();
        this.filter.mapFilterByAllies();
        this.sorting = parseSorting(params, sortingClass);
        this.sorting.checkAllowedSortFields();
        this.sorting.mapSortByAllies();
    }

    public static SearchRequestBuilder builder() {
        return new SearchRequestBuilder();
    }

    @SneakyThrows
    private <S extends Sorting> S parseSorting(MultiValueMap<String, String> params, Class<S> sortingClass) {
        List<SortingUnit> sorts = new ArrayList<>();
        S sortingObject = sortingClass.getDeclaredConstructor().newInstance();
        sortingObject.setSort(sorts);
        if (!params.containsKey(SORT_PARAM)) {
            return sortingObject;
        }
        for (String sort : params.get(SORT_PARAM)) {
            sorts.add(parseSort(sort));
        }
        return sortingObject;
    }

    private SortingUnit parseSort(String param) {
        if (!param.endsWith(":asc") && !param.endsWith(":desc")) {
            param += ":asc";
        }
        String[] parts = param.split(":",2);
        String field = parts[0];
        String order = parts[1].toLowerCase();
        return new SortingUnit(field, order);
    }

    @SneakyThrows
    private <F extends Filter> F parseFilter(MultiValueMap<String, String> params, Class<F> filterClass) {
        F filterObject = filterClass.getDeclaredConstructor().newInstance();
        List<FilterBasicOperation> filters = new ArrayList<>();
        for (var entry : params.entrySet()) {
            if (NON_FILTER_KEYS.contains(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                filters.add(parseFilter(entry.getKey(), value));
            }
        }
        filterObject.setOperations(filters);
        return filterObject;
    }


    private FilterBasicOperation parseFilter(String param, String value) {
        String operation = "=";
        String pureValue = value;

        Matcher matcher = FILTER_PATTERN.matcher(value);
        if (matcher.matches()) {
            operation = matcher.group(1);
            pureValue = matcher.group(2);

            if (FILTER_PREFIX_MAPPING.containsKey(operation)) {
                operation = FILTER_PREFIX_MAPPING.get(operation);
            }
        }

        FilterOperation filterOperation = FilterOperation.parse(operation);
        Object convertedValue = convertValueByOperation(filterOperation, pureValue);
        return new FilterBasicOperation(param, filterOperation, pureValue);
    }

    private Object convertValueByOperation(FilterOperation filterOperation, String stringValue) {
        return switch (filterOperation) {
            case IN, NOT_IN -> new ArrayList<>(Arrays.asList(stringValue.split(";")));
            default -> stringValue;
        };
    }

    private Pagination parsePagination(MultiValueMap<String, String> params) {
        if (!params.containsKey(PAGE_PARAM) || !params.containsKey(PAGE_SIZE_PARAM)) {
            return Pagination.unpaged();
        }
        int page = Integer.parseInt(Objects.requireNonNull(params.getFirst(PAGE_PARAM)));
        int pageSize = Integer.parseInt(Objects.requireNonNull(params.getFirst(PAGE_SIZE_PARAM)));
        return Pagination.of(page, pageSize);
    }

    public <F extends Filter<?>> F getFilter() {
        return (F) filter;
    }

    public <S extends Sorting> S getSorting() {
        return (S) sorting;
    }


    public static class SearchRequestBuilder<F extends Filter<?>, S extends Sorting> {
        private MultiValueMap<String, String> params;
        private Class<F> filterClass;
        private Class<S> sortingClass;

        SearchRequestBuilder() {
        }

        public SearchRequestBuilder params(MultiValueMap<String, String> params) {
            this.params = params;
            return this;
        }

        public SearchRequestBuilder filterClass(Class<F> filterClass) {
            this.filterClass = filterClass;
            return this;
        }

        public SearchRequestBuilder sortingClass(Class<S> sortingClass) {
            this.sortingClass = sortingClass;
            return this;
        }

        public SearchRequest build() {
            return new SearchRequest(this.params, this.filterClass, this.sortingClass);
        }

        public String toString() {
            return "SearchRequest.SearchRequestBuilder(params=" + this.params + ", filterClass=" + this.filterClass + ", sortingClass=" + this.sortingClass + ")";
        }
    }
}
