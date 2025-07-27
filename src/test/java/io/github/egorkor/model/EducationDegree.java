package io.github.egorkor.model;

import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@CatalogMeta(verboseName = "Уровни образования")
public class EducationDegree {
    @Id
    @AttributeMeta(verboseName = "Код уровня", required = true)
    private Integer code;
    @AttributeMeta(verboseName = "Полное наименование", required = true)
    private String name;
}
