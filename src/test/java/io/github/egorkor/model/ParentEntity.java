package io.github.egorkor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(fetch = FetchType.LAZY)
    private List<LazyChildEntity> lazyChildren;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CascadeLazyChildEntity> cascadeLazyChildren;
}
