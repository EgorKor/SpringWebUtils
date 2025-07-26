package io.github.egorkor.tests.utils.dto;

import lombok.Data;

import java.util.List;

@Data
public class DepartmentDto {
    private Long id;
    private String name;
    private List<EmployeeDto> employees;
}
