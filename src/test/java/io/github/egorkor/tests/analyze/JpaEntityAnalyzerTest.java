package io.github.egorkor.tests.analyze;

import io.github.egorkor.webutils.analyze.JpaCatalogEntityMetaAnalyzer;
import io.github.egorkor.webutils.analyze.ModelMeta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Map;

@DataJpaTest
public class JpaEntityAnalyzerTest {
    @Autowired
    private EntityManager entityManager;

    private JpaCatalogEntityMetaAnalyzer analyzer;

    @BeforeEach
    public void setUp() {
        analyzer = new JpaCatalogEntityMetaAnalyzer(entityManager);
    }

    @Test
    public void test1(){
        Map<Class<?>, ModelMeta> meta = analyzer.getMeta();
        System.out.println(meta.keySet().size());

    }




}
