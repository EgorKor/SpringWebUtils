package io.github.egorkor.repository;

import io.github.egorkor.model.TestEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Profile("test")
@Repository
public interface TestEntityRepository extends JpaRepository<TestEntity, Long>, JpaSpecificationExecutor<TestEntity> {
}
