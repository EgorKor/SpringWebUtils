package io.github.egorkor.webutils.template.jpa;

import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.query.Filter;
import io.github.egorkor.webutils.query.PageableResult;
import io.github.egorkor.webutils.query.Pagination;
import io.github.egorkor.webutils.query.Sorting;
import io.github.egorkor.webutils.service.sync.CRUDLService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

@Transactional
@RequiredArgsConstructor
public class JpaService<T, ID> implements CRUDLService<T, ID> {
    protected final JpaRepository<T, ID> jpaRepository;
    protected final JpaSpecificationExecutor<T> jpaSpecificationExecutor;
    private Class<T> type;

    {
        Type superclass = getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) superclass;
        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        this.type = (Class<T>) typeArgument;
    }

    private String getEntityName() {
        return type == null ? "" : type.getSimpleName();
    }

    @Override
    public PageableResult<List<T>> getAll(Filter<T> filter, Sorting sorting, Pagination pagination) {
        return PageableResult.of(
                jpaSpecificationExecutor.findAll(filter, pagination.toJpaPageable(sorting)).toList(),
                jpaSpecificationExecutor.count(filter),
                pagination.getSize()
        );
    }

    @Override
    public T getById(ID id) {
        return jpaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Сущность " + getEntityName() + " c id = " + id + " не найдена."));
    }

    @Override
    public T create(T model) {
        return jpaRepository.save(model);
    }

    @Override
    public T update(T model) {
        return jpaRepository.save(model);
    }

    @Override
    public void delete(ID id) {
        jpaRepository.deleteById(id);
    }
}
