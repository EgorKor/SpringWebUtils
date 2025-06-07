package io.github.egorkor;

import io.github.egorkor.webutils.template.AbstractCRUDLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

@Profile("test")
@Service
public class TestEntityServiceImpl extends AbstractCRUDLService<TestEntity, Long> implements TestEntityService{

    @Autowired
    public TestEntityServiceImpl(JpaRepository<TestEntity, Long> jpaRepository, JpaSpecificationExecutor<TestEntity> jpaSpecificationExecutor) {
        super(jpaRepository, jpaSpecificationExecutor);
    }

}
