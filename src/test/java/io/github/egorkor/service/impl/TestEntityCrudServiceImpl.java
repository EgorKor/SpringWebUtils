package io.github.egorkor.service.impl;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.service.TestEntityService;
import io.github.egorkor.webutils.template.jpa.JpaCrudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Profile("test")
@Service
public class TestEntityCrudServiceImpl extends JpaCrudService<TestEntity, Long> implements TestEntityService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public TestEntityCrudServiceImpl(JpaRepository<TestEntity, Long> jpaRepository,
                                     JpaSpecificationExecutor<TestEntity> jpaSpecificationExecutor,
                                     ApplicationEventPublisher eventPublisher,
                                     TransactionTemplate transactionTemplate) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate);
    }

    @Override
    public EntityManager getPersistenceAnnotatedEntityManager() {
        return entityManager;
    }


}
