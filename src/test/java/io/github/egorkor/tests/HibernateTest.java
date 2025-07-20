package io.github.egorkor.tests;

import io.github.egorkor.model.Order;
import io.github.egorkor.model.User;
import io.github.egorkor.repository.OrderRepository;
import io.github.egorkor.repository.UserRepository;
import io.github.egorkor.service.UserService;
import io.github.egorkor.service.impl.UserServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;


@Import({UserServiceImpl.class, LocalValidatorFactoryBean.class})
@ActiveProfiles("test")
@DataJpaTest
public class HibernateTest {
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private EntityManager entityManager;
    private Statistics stats;

    @BeforeTransaction
    public void setUp() {
        userService.deleteAll();
        orderRepository.deleteAll();

        User user = User.generateUser(1L);
        user = userRepository.save(user);

        Order order1 = Order.builder().id(1L).name("name1").build();
        order1.setUser(user);
        Order order2 = Order.builder().id(2L).name("name2").build();
        order2.setUser(user);

        order1 = orderRepository.save(order1);
        order2 = orderRepository.save(order2);
        // Синхронизация обеих сторон!
        user.addOrder(order1); // order1.setUser(user) + user.getOrders().add(order1)
        user.addOrder(order2);


    }

    @Transactional
    @Test
    public void testLazyInit() {
        User user = userRepository.findById(1L).orElseThrow();
        System.out.println(user.getOrders());
        System.out.println("HELLO");
    }


}
