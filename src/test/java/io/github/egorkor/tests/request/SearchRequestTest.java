package io.github.egorkor.tests.request;

import io.github.egorkor.model.User;
import io.github.egorkor.webutils.exception.InvalidParameterException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.SearchRequest;
import io.github.egorkor.webutils.queryparam.Sorting;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static io.github.egorkor.webutils.queryparam.SearchRequest.*;
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
        assertIterableEquals(List.of("id:asc", "name:desc"), sorting.getSort());
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
        Assertions.assertIterableEquals(List.of(
                        "name:=:Egor",
                        "name:like:Egor",
                        "name:not_like:Egor",
                        "name:is:null",
                        "name:is_not:null",
                        "name:in:Eg;or;ic",
                        "name:is:true",
                        "name:is_not:true",
                        "name:is:false",
                        "name:is_not:false",
                        "name.length():>:10",
                        "name.length():>=:10",
                        "name.length():<:10",
                        "name.length():<=:10",
                        "name.length():!=:10"),

                filter.getFilter());
    }

    @Test
    public void testSortDerived() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(SORT_PARAM, "id");
        params.add(SORT_PARAM, "name:desc");
        SearchRequest searchRequest = new SearchRequest(params, Filter.class, SortParams.class);
        SortParams sorting = searchRequest.getSorting();
        assertIterableEquals(List.of("id:asc", "name:desc"), sorting.getSort());
        assertThrows(InvalidParameterException.class, () -> sorting.toSQLSort());
    }

    @Test
    public void testFilterDerived() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Egor");
        params.add("value", "like:Egor");
        SearchRequest searchRequest = SearchRequest.builder()
                .params(params)
                .filterClass(FilterParams.class)
                .build();
        FilterParams filterParams = searchRequest.getFilter();
        assertIterableEquals(List.of("name:=:Egor","value:like:Egor"),
                filterParams.getFilter());
        assertThrows(InvalidParameterException.class, () -> filterParams.toSQLFilter());
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
