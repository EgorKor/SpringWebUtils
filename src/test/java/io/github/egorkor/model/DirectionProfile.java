package io.github.egorkor.model;

import io.github.egorkor.service.impl.StructureDepartmentServiceImpl;
import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import io.github.egorkor.webutils.annotations.Choices;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@CatalogMeta(verboseName = "Профили")
@ToString
public class DirectionProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 50)
    @AttributeMeta(required = true, verboseName = "Название профиля")
    private String name;

    @Choices(StructureDepartmentServiceImpl.class)
    @AttributeMeta(required = true,
            verboseName = "Структурное подразделение",
            placeholder = "Кафедра 'Информатика и вычислительная техника'")
    @ManyToOne(optional = false)
    private StructureDepartment structureDepartment;


    @ManyToMany(mappedBy = "profiles")
    private List<EducationProgram> programs;
}
