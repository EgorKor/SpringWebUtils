package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.User;
import io.github.egorkor.repository.UserRepository;
import io.github.egorkor.service.UserService;
import io.github.egorkor.service.impl.UserServiceImpl;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import jakarta.persistence.EntityManager;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static io.github.egorkor.webutils.queryparam.Filter.fb;


@Import({UserServiceImpl.class, LocalValidatorFactoryBean.class})
@ActiveProfiles("test")
@DataJpaTest
public class UserServiceTests {
    @Autowired
    private UserService userService;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private EntityManager entityManager;
    private Statistics stats;

    @Autowired
    private UserRepository userRepository;

    @BeforeTransaction
    public void beforeTransaction() {
        userService.deleteAll();
        User.generateUsers(1, 50).forEach(userService::create);
        this.stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        entityManager.clear();
    }

    @Test
    public void deleteById(){
        stats.setStatisticsEnabled(true);
        userService.deleteById(1L);
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(2, stats.getPrepareStatementCount());
    }

    @Test
    public void deleteAll(){
        stats.setStatisticsEnabled(true);
        userService.deleteAll();
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(2, stats.getPrepareStatementCount());
    }

    @Test
    public void deleteByIdNotFound(){
        stats.setStatisticsEnabled(true);
        Assertions.assertThrows(ResourceNotFoundException.class, () -> userService.deleteById(1000L));
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(2, stats.getPrepareStatementCount());
    }

    @Test
    public void testFindAll() {
        stats.setStatisticsEnabled(true);
        var res = userService.getPage(Filter.empty(), Sorting.unsorted(), Pagination.unpaged());
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(1, stats.getPrepareStatementCount());
    }

    @Test
    public void testFindByIdWithJoin() {
        stats.setStatisticsEnabled(true);
        var res = userService.getById(1L, "orders");
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(1, stats.getPrepareStatementCount());
        Assertions.assertNotNull(res.getOrders());
    }

    @Test
    public void softDeleteByFilter() {
        stats.setStatisticsEnabled(true);
        userService.softDeleteByFilter(fb.and(fb.greater("id", "30"))
                .build());
        var res = userService.getPage(Filter.empty(), Sorting.unsorted(), Pagination.unpaged());
        Assertions.assertEquals(2, stats.getPrepareStatementCount());
        Assertions.assertEquals(30, res.getData().size());
        stats.setStatisticsEnabled(false);
    }

    @Test
    public void recoverByFilter() {
        stats.setStatisticsEnabled(true);
        userService.softDeleteByFilter(fb.and(
                fb.lessOrEquals("id", "10")
        ).build());
        var res = userService.getPage(Filter.empty(), Sorting.unsorted(), Pagination.unpaged());
        Assertions.assertEquals(res.getData().size(), 40);
        userService.restoreByFilter(fb.and(
                fb.lessOrEquals("id", "5")
        ).build());
        res = userService.getPage(Filter.empty(), Sorting.unsorted(), Pagination.unpaged());
        Assertions.assertEquals(res.getData().size(), 45);
        Assertions.assertEquals(4, stats.getPrepareStatementCount());
    }

    @Test
    public void testPaginationRequest() {
        stats.setStatisticsEnabled(true);
        List<User> users = userService.getPage(Filter.empty(), Sorting.unsorted(), Pagination.of(0, 10)).getData();
        stats.setStatisticsEnabled(false);
        Assertions.assertEquals(2, stats.getPrepareStatementCount());
        Assertions.assertEquals(10, users.size());
    }


}
