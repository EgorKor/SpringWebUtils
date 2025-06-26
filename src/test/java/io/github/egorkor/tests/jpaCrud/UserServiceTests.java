package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.User;
import io.github.egorkor.service.UserService;
import io.github.egorkor.service.impl.UserServiceImpl;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@Import(UserServiceImpl.class)
@ActiveProfiles("test")
@DataJpaTest
public class UserServiceTests {
    @Autowired
    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService.deleteAll();
        User.generateUsers(1, 50).forEach(userService::create);
    }

    @Test
    public void testFindAll() {
        var res = userService.getAll(Filter.emptyFilter(), Sorting.unsorted(), Pagination.unpaged());
        Assertions.assertEquals(res.getData().size(), 50);
    }


}
