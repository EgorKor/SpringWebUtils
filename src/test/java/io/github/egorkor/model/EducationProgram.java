package io.github.egorkor.model;

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
public class EducationProgram {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @JoinTable(joinColumns = {@JoinColumn(name = "profile_id")},
            inverseJoinColumns = {@JoinColumn(name = "program_id")})
    @ManyToMany
    private List<DirectionProfile> profiles;
}
