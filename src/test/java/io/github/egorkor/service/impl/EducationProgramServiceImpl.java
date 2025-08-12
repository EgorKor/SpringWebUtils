package io.github.egorkor.service.impl;

import io.github.egorkor.model.EducationProgram;
import io.github.egorkor.service.EducationProgramService;
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
public class EducationProgramServiceImpl
        extends JpaCrudService<EducationProgram, Long>
        implements EducationProgramService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public EducationProgramServiceImpl(JpaRepository<EducationProgram, Long> jpaRepository, JpaSpecificationExecutor<EducationProgram> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate, validator);
    }

    @Override
    public EntityManager getPersistenceAnnotatedEntityManager() {
        return entityManager;
    }
}
