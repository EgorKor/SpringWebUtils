package io.github.egorkor;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class TestEntity {
    @Id
    private Long id;
    private String name;
    @OneToMany
    private List<TestNestedEntity> nested;
}
