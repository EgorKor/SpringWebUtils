package io.github.egorkor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Check;

@Data
@AllArgsConstructor
public class DepartmentChoice {
    private Long id;
    private String name;
}
