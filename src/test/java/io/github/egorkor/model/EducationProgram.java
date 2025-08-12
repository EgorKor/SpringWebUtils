package io.github.egorkor.model;

import io.github.egorkor.service.impl.DirectionProfileServiceImpl;
import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import io.github.egorkor.webutils.annotations.Choices;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@CatalogMeta(verboseName = "Образовательные программы")
public class EducationProgram {
    @Id
    @GeneratedValue
    private Long id;

    @AttributeMeta(verboseName = "Название",required = true)
    private String name;

    @JoinTable(joinColumns = {@JoinColumn(name = "profile_id")},
            inverseJoinColumns = {@JoinColumn(name = "program_id")})
    @Choices(DirectionProfileServiceImpl.class)
    @ManyToMany
    private List<DirectionProfile> profiles;
}
