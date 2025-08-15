package io.github.egorkor.model;

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
@ToString
public class DirectionProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 50)
    private String name;

    @ManyToOne(optional = false)
    private StructureDepartment structureDepartment;


    @ManyToMany(mappedBy = "profiles")
    private List<EducationProgram> programs;
}
