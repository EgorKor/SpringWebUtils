package io.github.egorkor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class TestNestedEntity {
    @Id
    private Long id;
    @ManyToOne
    private TestEntity parent;
}
