package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import jakarta.persistence.Column;
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
    @Column(name = "_name")
    private String name;
    @OneToMany
    private List<TestNestedEntity> nested;
    @SoftDeleteFlag
    private Boolean isDeleted;
}
