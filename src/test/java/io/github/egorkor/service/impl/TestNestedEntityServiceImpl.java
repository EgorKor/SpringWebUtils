package io.github.egorkor.service.impl;

import io.github.egorkor.model.TestNestedEntity;
import io.github.egorkor.service.TestNestedEntityService;
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
public class TestNestedEntityServiceImpl extends JpaCrudService<TestNestedEntity, Long> implements TestNestedEntityService {
    @PersistenceContext
    private EntityManager entityManager;

    public TestNestedEntityServiceImpl(JpaRepository<TestNestedEntity, Long> jpaRepository, JpaSpecificationExecutor<TestNestedEntity> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate, validator);
    }


    @Override
    public EntityManager getPersistenceAnnotatedEntityManager() {
        return entityManager;
    }
}
