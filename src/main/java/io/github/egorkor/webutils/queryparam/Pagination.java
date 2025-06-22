package io.github.egorkor.webutils.queryparam;


import io.github.egorkor.webutils.queryparam.utils.DatabaseType;
import io.github.egorkor.webutils.queryparam.utils.DriverUtils;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Параметр запроса для пагинации запрашиваемых ресурсов.
 * Пример использования в контроллере:
 * <pre>{@code
 * public void controllerMethod(@RequestParam Pagination pagination)
 * }</pre>
 * Пример использования с JPA репозиториями сортировка и пагинация
 * <pre>
 * {@code
 * public List<Entity> query(SortParams sort, Pagination pagination){
 *     repository.findAll(pagination.toJpaPageable(sort));
 * }
 * }
 * </pre>
 *
 * @author EgorKor
 * @since 2025
 */
@Data
public class Pagination {
    public static final int ALL_CONTENT_SIZE = -1;
    public static final int DEFAULT_PAGE = 0;
    private static DatabaseType dbType = DriverUtils.getActiveDatabaseType();

    private int size = 10;
    private int page = DEFAULT_PAGE;


    public Pageable toJpaPageable() {
        if (size == ALL_CONTENT_SIZE) {
            return Pageable.unpaged();
        }
        return PageRequest.of(page, size);
    }

    public Pageable toJpaPageable(Sort sort) {
        if (size == ALL_CONTENT_SIZE) {
            return Pageable.unpaged(sort);
        }
        return PageRequest.of(page, size, sort);
    }

    public Pageable toJpaPageable(Sorting sorting) {
        if (size == ALL_CONTENT_SIZE) {
            return Pageable.unpaged(sorting.toJpaSort());
        }
        return PageRequest.of(page, size, sorting.toJpaSort());
    }


    public String toSqlPageable() {
        return toSqlPageable(dbType);
    }

    public String toSqlPageable(DatabaseType dbType) {
        if (size == ALL_CONTENT_SIZE) return "";

        long offset = (long) page * size;

        return switch (dbType) {
            case POSTGRESQL, H2, SQLITE, MYSQL, MARIADB -> "LIMIT %d OFFSET %d".formatted(size, offset);
            case ORACLE, SQL_SERVER -> "OFFSET %d ROWS FETCH NEXT %d ROWS ONLY".formatted(offset, size);
            case DB2 -> "OFFSET %d ROWS FETCH FIRST %d ROWS ONLY".formatted(offset, size);
            default -> throw new UnsupportedOperationException("Unsupported database type");
        };
    }


}
