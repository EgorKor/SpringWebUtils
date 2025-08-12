package io.github.egorkor.repository;

import io.github.egorkor.model.DirectionProfile;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
@Profile("test")
public interface DirectionProfileRepository extends JpaRepository<DirectionProfile, Long>, JpaSpecificationExecutor<DirectionProfile> {
}
