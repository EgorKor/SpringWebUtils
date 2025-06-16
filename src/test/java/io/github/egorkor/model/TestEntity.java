package io.github.egorkor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
public class TestEntity {
    @Id
    private Long id;
    private String name;
    @OneToMany
    private List<TestNestedEntity> nested;
    private Boolean isDeleted;
}
