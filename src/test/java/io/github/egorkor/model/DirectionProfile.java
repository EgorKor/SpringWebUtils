package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
    @Size(max = 50)
    @AttributeMeta(required = true, verboseName = "Название профиля")
    private String name;
    @AttributeMeta(required = true, verboseName = "Структурное подразделение")
    @ManyToOne(optional = false)
    private StructureDepartment structureDepartment;
}
