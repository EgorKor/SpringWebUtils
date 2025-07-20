package io.github.egorkor.repository;

import io.github.egorkor.model.LazyChildEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public interface LazyChildEntityRepository extends JpaRepository<LazyChildEntity, Long> {
}
