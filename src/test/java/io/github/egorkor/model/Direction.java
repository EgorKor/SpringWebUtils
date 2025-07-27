package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@CatalogMeta(verboseName = "Направления подготовки")
public class Direction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @AttributeMeta(required = true, verboseName = "Код направления")
    private Integer code;

    @AttributeMeta(required = true, verboseName = "Название направления")
    private String name;

    @AttributeMeta(required = true,verboseName = "УГНП")
    @ManyToOne(optional = false)
    private ConsolidatedGroupOfDirections group;

    @AttributeMeta(required = true, verboseName = "Уровень образования")
    @ManyToOne(optional = false)
    private EducationDegree degree;

}
