package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@DynamicUpdate
public class TestEntity {
    @Id
    private Long id;
    @Column(name = "_name")
    private String name;
    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<TestNestedEntity> nested;
    private Integer nullableProperty;
    private Boolean flag;
    @ElementCollection
    private List<Integer> nums;
    @ElementCollection
    private List<String> tags;
    @ElementCollection
    private List<Tag> enumTags;
    @SoftDeleteFlag
    private Boolean isDeleted;
}
