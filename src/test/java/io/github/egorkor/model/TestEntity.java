package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import jakarta.persistence.*;
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
    @ElementCollection
    private List<Integer> nums;
    @ElementCollection
    private List<String> tags;
    @ElementCollection
    private List<Tag> enumTags;
    @SoftDeleteFlag
    private Boolean isDeleted;
}
