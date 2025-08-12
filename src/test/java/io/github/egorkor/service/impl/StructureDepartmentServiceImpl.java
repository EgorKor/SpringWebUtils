package io.github.egorkor.service.impl;

import io.github.egorkor.model.DepartmentChoice;
import io.github.egorkor.model.StructureDepartment;
import io.github.egorkor.service.StructureDepartmentService;
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

import java.util.List;

@Profile("test")
@Service
public class StructureDepartmentServiceImpl
        extends JpaCrudService<StructureDepartment, Long>
        implements StructureDepartmentService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public StructureDepartmentServiceImpl(JpaRepository<StructureDepartment, Long> jpaRepository, JpaSpecificationExecutor<StructureDepartment> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate, validator);
    }

    @Override
    public EntityManager getPersistenceAnnotatedEntityManager() {
        return entityManager;
    }

    @Override
    public List<Object> getChoices() {
        return getList().stream()
                .map((dep) -> (Object)new DepartmentChoice(dep.getId(), dep.getName()))
                .toList();
    }
}
