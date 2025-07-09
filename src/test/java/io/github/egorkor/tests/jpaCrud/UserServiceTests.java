package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.User;
import io.github.egorkor.service.UserService;
import io.github.egorkor.service.impl.UserServiceImpl;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.BeforeTransaction;


@Import(UserServiceImpl.class)
@ActiveProfiles("test")
@DataJpaTest
public class UserServiceTests {
    @Autowired
    private UserService userService;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    private Statistics stats;

    @BeforeTransaction
    public void beforeTransaction() {
        userService.deleteAll();
        User.generateUsers(1, 50).forEach(userService::create);
        this.stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
    }

    @Test
    public void testFindAll() {
        stats.setStatisticsEnabled(true);
        var res = userService.getAll(Filter.emptyFilter(), Sorting.unsorted(), Pagination.unpaged());
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(1, stats.getPrepareStatementCount());
    }

    @Test
    public void testFindByIdWithJoin() {
        stats.setStatisticsEnabled(true);
        var res = userService.getByIdWithFilter(1L, Filter
                .emptyFilter()
                .withFetchJoin("orders"));
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(1, stats.getPrepareStatementCount());
        Assertions.assertNotNull(res.getOrders());
    }

    @Test
    public void softDeleteByFilter() {
        stats.setStatisticsEnabled(true);
        userService.softDeleteByFilter(Filter.builder()
                .greater("id", "30")
                .build());
        var res = userService.getAll(Filter.emptyFilter(), Sorting.unsorted(), Pagination.unpaged());
        Assertions.assertEquals(2, stats.getPrepareStatementCount());
        Assertions.assertEquals(30, res.getData().size());
        stats.setStatisticsEnabled(false);
    }

    @Test
    public void recoverByFilter() {
        stats.setStatisticsEnabled(true);
        userService.softDeleteByFilter(Filter.builder()
                .lessOrEquals("id","10")
                .build());
        var res = userService.getAll(Filter.emptyFilter(), Sorting.unsorted(), Pagination.unpaged());
        Assertions.assertEquals(res.getData().size(), 40);
        userService.restoreByFilter(Filter.builder()
                .lessOrEquals("id","5")
                .build());
        res = userService.getAll(Filter.emptyFilter(), Sorting.unsorted(), Pagination.unpaged());
        Assertions.assertEquals(res.getData().size(), 45);
        Assertions.assertEquals(4, stats.getPrepareStatementCount());

    }


}
