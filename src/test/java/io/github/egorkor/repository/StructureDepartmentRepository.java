package io.github.egorkor.repository;

import io.github.egorkor.model.StructureDepartment;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ActiveProfiles;

@Repository
@Profile("test")
public interface StructureDepartmentRepository extends JpaRepository<StructureDepartment, Long>,
        JpaSpecificationExecutor<StructureDepartment> {
}
