package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@CatalogMeta(verboseName = "Профили")
public class DirectionProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @AttributeMeta(required = true, verboseName = "Название профиля")
    private String name;
    @AttributeMeta(required = true, verboseName = "Структурное подразделение")
    @ManyToOne(optional = false)
    private StructureDepartment structureDepartment;
}
