package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.Order;
import io.github.egorkor.model.User;
import io.github.egorkor.service.OrderService;
import io.github.egorkor.service.UserService;
import io.github.egorkor.service.impl.OrderServiceImpl;
import io.github.egorkor.service.impl.UserServiceImpl;
import io.github.egorkor.webutils.queryparam.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

@Import({OrderServiceImpl.class, UserServiceImpl.class, LocalValidatorFactoryBean.class})
@ActiveProfiles("test")
@DataJpaTest
public class OrderServiceTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;


    @BeforeTransaction
    public void setup() {
        User user1 = userService.create(
                User.generateUser(1L)
        );
        User user2 = userService.create(
                User.generateUser(2L)
        );
        orderService.create(
                Order.builder()
                        .id(1L)
                        .user(user1)
                        .build()
        );
        orderService.create(
                Order.builder()
                        .id(2L)
                        .user(user2)
                        .build()
        );
    }

    @Test
    public void testOrderNPlus1() {
        List<Order> orders = orderService.getList(Filter.empty());
        System.out.println(orders.size());
    }

}
