package io.github.egorkor.tests.utils;


import io.github.egorkor.tests.utils.dto.Department;
import io.github.egorkor.tests.utils.dto.DepartmentDto;
import io.github.egorkor.tests.utils.dto.Employee;
import io.github.egorkor.tests.utils.dto.EmployeeDto;
import io.github.egorkor.webutils.dto.DtoMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DtoMappingTests {
    private final DtoMapper dtoMapper = new DtoMapper();



    @Test
    void shouldMapDepartmentWithEmployees() {
        Department department = new Department();
        department.setId(1L);
        department.setName("IT");

        Employee emp1 = new Employee();
        emp1.setId(101L);
        emp1.setName("John Doe");
        emp1.setDepartment(department);

        Employee emp2 = new Employee();
        emp2.setId(102L);
        emp2.setName("Jane Smith");
        emp2.setDepartment(department);

        department.setEmployees(List.of(emp1, emp2));

        DepartmentDto dto = dtoMapper.toDto(department, DepartmentDto.class);

        assertEquals(1L, dto.getId());
        assertEquals("IT", dto.getName());
        assertEquals(2, dto.getEmployees().size());
        assertEquals("John Doe", dto.getEmployees().get(0).getName());
        assertEquals(1L, dto.getEmployees().get(0).getDepartmentId());
    }

    // 2. Тест маппинга Employee -> EmployeeDto
    @Test
    void shouldMapEmployeeWithDepartmentId() {
        Department department = new Department();
        department.setId(2L);

        Employee employee = new Employee();
        employee.setId(201L);
        employee.setName("Alice Brown");
        employee.setDepartment(department);

        EmployeeDto dto = dtoMapper.toDto(employee, EmployeeDto.class);

        assertEquals(201L, dto.getId());
        assertEquals("Alice Brown", dto.getName());
        assertEquals(2L, dto.getDepartmentId());
    }

    // 3. Тест обратного маппинга DepartmentDto -> Department
    @Test
    void shouldReverseMapDepartmentDto() {
        EmployeeDto empDto1 = new EmployeeDto();
        empDto1.setId(301L);
        empDto1.setName("Bob Johnson");

        DepartmentDto deptDto = new DepartmentDto();
        deptDto.setId(3L);
        deptDto.setName("HR");
        deptDto.setEmployees(List.of(empDto1));

        Department department = dtoMapper.toModel(deptDto, Department.class);

        assertEquals(3L, department.getId());
        assertEquals("HR", department.getName());
        assertEquals(1, department.getEmployees().size());
        assertEquals("Bob Johnson", department.getEmployees().get(0).getName());
    }

    // 4. Тест циклических зависимостей
    @Test
    void shouldHandleCircularDependencies() {
        Department department = new Department();
        department.setId(4L);

        Employee employee = new Employee();
        employee.setId(401L);
        employee.setDepartment(department);

        department.setEmployees(List.of(employee));

        // Преобразование без зацикливания
        DepartmentDto dto = dtoMapper.toDto(department, DepartmentDto.class);

        assertEquals(1, dto.getEmployees().size());
        assertEquals(4L, dto.getEmployees().get(0).getDepartmentId());
    }

    // 5. Тест с пустой коллекцией
    @Test
    void shouldMapEmptyEmployeeList() {
        Department department = new Department();
        department.setEmployees(Collections.emptyList());

        DepartmentDto dto = dtoMapper.toDto(department, DepartmentDto.class);

        assertNotNull(dto.getEmployees());
        assertTrue(dto.getEmployees().isEmpty());
    }

    // 6. Тест маппинга null-значений
    @Test
    void shouldMapNullRelations() {
        Employee employee = new Employee();
        employee.setId(501L);
        // department не установлен

        EmployeeDto dto = dtoMapper.toDto(employee, EmployeeDto.class);

        assertEquals(501L, dto.getId());
        assertNull(dto.getDepartmentId());
    }

    // 7. Тест маппинга коллекции Employee -> EmployeeDto
    @Test
    void shouldMapEmployeeList() {
        Department dept = new Department();
        dept.setId(5L);

        List<Employee> employees = List.of(
                new Employee(601L, "Mike", dept),
                new Employee(602L, "Sarah", dept)
        );
 
        List<EmployeeDto> dtos = dtoMapper.toDto(employees, EmployeeDto.class);

        assertEquals(2, dtos.size());
        assertEquals("Mike", dtos.getFirst().getName());
        assertEquals(5L, dtos.getFirst().getDepartmentId());
    }

    // 8. Тест маппинга с ленивой загрузкой (Mock)
    @Test
    void shouldHandleLazyLoading() {
        Department department = mock(Department.class);
        when(department.getId()).thenReturn(6L);

        Employee employee = new Employee();
        employee.setId(701L);
        employee.setDepartment(department); // Ленивая загрузка

        EmployeeDto dto = dtoMapper.toDto(employee, EmployeeDto.class);

        assertEquals(701L, dto.getId());
        assertEquals(6L, dto.getDepartmentId());
        verify(department, times(1)).getId(); // Проверяем, что getId() был вызван
    }

    // 9. Тест кастомного маппинга для DTO -> Entity
    @Test
    void shouldCustomMapDtoToEntity() {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(801L);
        dto.setDepartmentId(8L);

        Employee employee = dtoMapper.toModel(dto, Employee.class);

        assertEquals(801L, employee.getId());
    }







}
