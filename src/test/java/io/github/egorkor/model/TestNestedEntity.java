package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class TestNestedEntity {
    @Id
    private Long id;
    @ManyToOne
    private TestEntity parent;
    @SoftDeleteFlag
    private LocalDateTime deletedAt;
}
