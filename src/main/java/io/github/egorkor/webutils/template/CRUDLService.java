package io.github.egorkor.template;

import io.github.egorkor.query.Filter;
import io.github.egorkor.query.PageableResult;
import io.github.egorkor.query.Pagination;
import io.github.egorkor.query.Sorting;

import java.util.List;

public interface CRUDLService<T, ID> {
    PageableResult<List<T>> getAll(Filter<T> filter, Sorting sorting, Pagination pagination);

    T getById(ID id);

    T create(T model);

    T update(T model);

    void delete(ID id);
}
