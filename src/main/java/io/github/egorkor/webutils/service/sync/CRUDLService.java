package io.github.egorkor.webutils.service.sync;

import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;

import java.util.List;

public interface CRUDLService<T, ID> {
    PageableResult<List<T>> getAll(Filter<T> filter, Sorting sorting, Pagination pagination);

    T getById(ID id);

    T create(T model);

    T update(T model);

    void delete(ID id);
}
