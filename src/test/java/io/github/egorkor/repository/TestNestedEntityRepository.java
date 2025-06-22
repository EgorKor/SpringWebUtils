package io.github.egorkor.repository;

import io.github.egorkor.model.TestNestedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TestNestedEntityRepository extends JpaRepository<TestNestedEntity, Long>, JpaSpecificationExecutor<TestNestedEntity> {
}
