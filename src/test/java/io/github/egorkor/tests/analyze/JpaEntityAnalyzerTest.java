package io.github.egorkor.tests.analyze;

import io.github.egorkor.dto.EducationProgramDto;
import io.github.egorkor.model.DirectionProfile;
import io.github.egorkor.model.EducationProgram;
import io.github.egorkor.model.StructureDepartment;
import io.github.egorkor.service.DirectionProfileService;
import io.github.egorkor.service.EducationProgramService;
import io.github.egorkor.service.StructureDepartmentService;
import io.github.egorkor.service.impl.DirectionProfileServiceImpl;
import io.github.egorkor.service.impl.EducationProgramServiceImpl;
import io.github.egorkor.service.impl.StructureDepartmentServiceImpl;
import io.github.egorkor.webutils.analyze.jpa.JpaCatalogEntityMetaAnalyzer;
import io.github.egorkor.webutils.analyze.jpa.ModelMeta;
import io.github.egorkor.webutils.analyze.jpa.ModelMetaHolder;
import io.github.egorkor.webutils.dto.DtoMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;

@Import({StructureDepartmentServiceImpl.class, LocalValidatorFactoryBean.class,
        DirectionProfileServiceImpl.class, EducationProgramServiceImpl.class,})
@ActiveProfiles("test")
@DataJpaTest
public class JpaEntityAnalyzerTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private StructureDepartmentService structureDepartmentService;
    @Autowired
    private DirectionProfileService directionProfileService;
    @Autowired
    private EducationProgramService educationProgramService;


    private static final DtoMapper dtoMapper = new DtoMapper();
    private ModelMetaHolder holder;

    private StructureDepartment structureDepartment;
    private EducationProgram educationProgram;
    private DirectionProfile directionProfile;

    @BeforeEach
    public void setUp() {
        structureDepartment = structureDepartmentService.create(
                StructureDepartment.builder()
                        .name("ИВТ")
                        .name("ИИТ")
                        .build()
        );
        directionProfile = directionProfileService.create(
                DirectionProfile.builder()
                        .name("Профиль ИВТ")
                        .structureDepartment(structureDepartment)
                        .build()
        );
        educationProgram = educationProgramService.create(
                EducationProgram.builder()
                        .name("Программа ИВТ 2025")
                        .profiles(List.of(directionProfile))
                        .build()
        );


        Map<Class<?>, ModelMeta> meta = JpaCatalogEntityMetaAnalyzer.getMeta(entityManager, applicationContext);
        this.holder = new ModelMetaHolder(meta);
    }

    @Test
    public void test1() {
        holder.getModelMetas().forEach(System.out::println);
    }

    @Test
    public void test2() {
        EducationProgramDto dto = dtoMapper.toDto(educationProgram, EducationProgramDto.class);
        System.out.println(dto);
        Assertions.assertNotNull(dto.getId());
        Assertions.assertEquals("Программа ИВТ 2025", dto.getName());
        Assertions.assertEquals(1, dto.getProfiles().size());
        Assertions.assertNotNull(dto.getProfiles().getFirst().getId());
        Assertions.assertEquals("Профиль ИВТ", dto.getProfiles().getFirst().getName());

        EducationProgram model = dtoMapper.toModel(dto, EducationProgram.class);
        System.out.println(model);

        System.out.println(holder.getModelMeta(EducationProgram.class).getMetaWithAttributeChoices());

    }


}
