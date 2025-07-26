package io.github.egorkor.repository;

import io.github.egorkor.model.TestingEntityBatching;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("test")
public interface BatchTestEntityRepository extends JpaRepository<TestingEntityBatching, Long>, JpaSpecificationExecutor<TestingEntityBatching> {
    List<TestingEntityBatching> findByName(String name);
}
