package io.github.egorkor.service.impl;

import io.github.egorkor.model.Order;
import io.github.egorkor.service.OrderService;
import io.github.egorkor.webutils.template.jpa.JpaCrudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Profile("test")
public class OrderServiceImpl extends JpaCrudService<Order, Long> implements OrderService {
    @PersistenceContext
    private EntityManager em;

    @Autowired
    public OrderServiceImpl(JpaRepository<Order, Long> jpaRepository, JpaSpecificationExecutor<Order> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate, validator);
    }


    @Override
    public EntityManager getPersistenceAnnotatedEntityManager() {
        return em;
    }
}
