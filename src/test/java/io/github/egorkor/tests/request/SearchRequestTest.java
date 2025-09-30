package io.github.egorkor.tests.request;

import io.github.egorkor.model.User;
import io.github.egorkor.webutils.exception.InvalidParameterException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.SearchRequest;
import io.github.egorkor.webutils.queryparam.Sorting;
import io.github.egorkor.webutils.queryparam.filterInternal.FilterBasicOperation;
import io.github.egorkor.webutils.queryparam.sortingInternal.SortingUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static io.github.egorkor.webutils.queryparam.SearchRequest.*;
import static io.github.egorkor.webutils.queryparam.filterInternal.FilterOperation.*;
import static org.junit.jupiter.api.Assertions.*;

public class SearchRequestTest {


    @Test
    public void testPagination() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(PAGE_PARAM, "1");
        params.add(PAGE_SIZE_PARAM, "10");
        SearchRequest searchRequest = new SearchRequest(params);
        Pagination pagination = searchRequest.getPagination();
        assertNotNull(pagination);
        assertTrue(pagination.isPaged());
        assertEquals(10, pagination.getSize());
        assertEquals(1, pagination.getPage());
    }

    @Test
    public void testSorting() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SORT_PARAM, "id");
        params.add(SORT_PARAM, "name:desc");
        SearchRequest searchRequest = new SearchRequest(params);
        Sorting sorting = searchRequest.getSorting();
        assertIterableEquals(List.of(
                new SortingUnit("id","asc"),
                new SortingUnit("name","desc")), sorting.getSort());
    }

    @Test
    public void testFilter() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Egor");
        params.add("name", "like:Egor");
        params.add("name", "not_like:Egor");
        params.add("name", "is:null");
        params.add("name", "is_not:null");
        params.add("name", "in:Eg;or;ic");
        params.add("name", "is:true");
        params.add("name", "is_not:true");
        params.add("name", "is:false");
        params.add("name", "is_not:false");
        params.add("name.length()", "gt:10");
        params.add("name.length()", "ge:10");
        params.add("name.length()", "lt:10");
        params.add("name.length()", "le:10");
        params.add("name.length()", "not_equals:10");
        SearchRequest searchRequest = new SearchRequest(params);
        Filter filter = searchRequest.getFilter();
        assertIterableEquals(List.of(
                        new FilterBasicOperation("name", EQUALS, "Egor"),
                        new FilterBasicOperation("name", LIKE, "Egor"),
                        new FilterBasicOperation("name", NOT_LIKE, "Egor"),
                        new FilterBasicOperation("name", IS, "null"),
                        new FilterBasicOperation("name", IS_NOT, "null"),
                        new FilterBasicOperation("name", IN, "Eg;or;ic"),
                        new FilterBasicOperation("name", IS, "true"),
                        new FilterBasicOperation("name", IS_NOT, "true"),
                        new FilterBasicOperation("name", IS, "false"),
                        new FilterBasicOperation("name", IS_NOT, "false"),
                        new FilterBasicOperation("name.length()", GT, "10"),
                        new FilterBasicOperation("name.length()", GTE, "10"),
                        new FilterBasicOperation("name.length()", LS, "10"),
                        new FilterBasicOperation("name.length()", LSE, "10"),
                        new FilterBasicOperation("name.length()", NOT_EQUALS, "10")),
                filter.getOperations());
    }

    @Test
    public void testSortDerived() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SORT_PARAM, "id");
        params.add(SORT_PARAM, "name:desc");
        assertThrows(InvalidParameterException.class,() -> {
            new SearchRequest(params, Filter.class, SortParams.class);
        });
    }

    @Test
    public void testFilterDerived() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Egor");
        params.add("value", "like:Egor");
        assertThrows(InvalidParameterException.class, () -> {
            SearchRequest searchRequest = SearchRequest.builder()
                    .params(params)
                    .filterClass(FilterParams.class)
                    .build();
        });
    }



    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class FilterParams extends Filter<User> {
        private String name;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class SortParams extends Sorting {
        private String name;

    }

}
