package io.github.egorkor.query;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
public class Pagination {
    private int size;
    private int page = 10;

    public Pageable toJpaPageable() {
        return PageRequest.of(page, size);
    }

    public Pageable toJpaPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }

    public Pageable toJpaPageable(Sorting sorting) {
        return PageRequest.of(page, size, sorting.toJpaSort());
    }

    public String toSQLPageable() {
        return "OFFSET %d LIMIT %d".formatted(page * size, size);
    }
}
