package io.github.egorkor.dto;

import io.github.egorkor.model.DirectionProfileChoice;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class EducationProgramDto {
    private Long id;
    private String name;
    private List<DirectionProfileChoice> profiles;
}
