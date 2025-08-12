package io.github.egorkor.service.impl;

import io.github.egorkor.model.DirectionProfile;
import io.github.egorkor.model.DirectionProfileChoice;
import io.github.egorkor.service.DirectionProfileService;
import io.github.egorkor.webutils.analyze.jpa.ChoicesSupplier;
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

@Service
@Profile("test")
public class DirectionProfileServiceImpl extends JpaCrudService<DirectionProfile, Long>
        implements DirectionProfileService, ChoicesSupplier {
    @PersistenceContext
    private EntityManager em;

    @Autowired
    public DirectionProfileServiceImpl(JpaRepository<DirectionProfile, Long> jpaRepository, JpaSpecificationExecutor<DirectionProfile> jpaSpecificationExecutor, ApplicationEventPublisher eventPublisher, TransactionTemplate transactionTemplate, Validator validator) {
        super(jpaRepository, jpaSpecificationExecutor, eventPublisher, transactionTemplate, validator);
    }

    @Override
    public EntityManager getPersistenceAnnotatedEntityManager() {
        return em;
    }

    @Override
    public List<Object> getChoices() {
        return getList().stream().map(
                profile -> (Object) new DirectionProfileChoice(profile.getId(), profile.getName())
        ).toList();
    }
}
