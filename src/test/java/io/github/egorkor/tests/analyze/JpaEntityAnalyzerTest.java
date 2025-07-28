package io.github.egorkor.tests.analyze;

import io.github.egorkor.model.DirectionProfile;
import io.github.egorkor.model.StructureDepartment;
import io.github.egorkor.service.StructureDepartmentService;
import io.github.egorkor.service.impl.StructureDepartmentServiceImpl;
import io.github.egorkor.webutils.analyze.jpa.JpaCatalogEntityMetaAnalyzer;
import io.github.egorkor.webutils.analyze.jpa.ModelMeta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@Import(StructureDepartmentServiceImpl.class)
@ActiveProfiles("test")
@DataJpaTest
public class JpaEntityAnalyzerTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private StructureDepartmentService structureDepartmentService;


    @BeforeEach
    public void setUp() {
        structureDepartmentService.create(
                StructureDepartment.builder()
                        .name("ИВТ")
                        .name("ИИТ")
                        .build()
        );
    }

    @Test
    public void test1(){
        Map<Class<?>, ModelMeta> meta = JpaCatalogEntityMetaAnalyzer.getMeta(entityManager, applicationContext);
        ModelMeta modelMeta = meta.get(DirectionProfile.class);
        modelMeta = modelMeta.getMetaWithAttributeChoices();
        System.out.println(modelMeta);
    }

}
