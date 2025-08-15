package io.github.egorkor.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Direction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer code;

    private String name;

    @ManyToOne(optional = false)
    private ConsolidatedGroupOfDirections group;

    @ManyToOne(optional = false)
    private EducationDegree degree;

}
