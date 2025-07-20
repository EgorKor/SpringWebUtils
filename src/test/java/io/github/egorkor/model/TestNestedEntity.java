package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class TestNestedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private TestEntity parent;
    @SoftDeleteFlag
    private LocalDateTime deletedAt;
}
