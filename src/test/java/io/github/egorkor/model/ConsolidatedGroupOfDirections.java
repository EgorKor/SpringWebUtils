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
@CatalogMeta(verboseName = "Укруплённые группы направлений подготовки")
public class ConsolidatedGroupOfDirections {
    @Id
    @AttributeMeta(verboseName = "Код группы", required = true)
    private Integer code;
    @AttributeMeta(verboseName = "Полное наименование", required = true)
    private String name;
}
