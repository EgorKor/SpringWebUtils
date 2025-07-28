package io.github.egorkor.tests.analyze;

import io.github.egorkor.webutils.analyze.jpa.JpaCatalogEntityMetaAnalyzer;
import io.github.egorkor.webutils.analyze.jpa.ModelMeta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;

import java.util.Map;

@DataJpaTest
public class JpaEntityAnalyzerTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void test1(){
        Map<Class<?>, ModelMeta> meta = JpaCatalogEntityMetaAnalyzer.getMeta(entityManager, applicationContext);
        JpaCatalogEntityMetaAnalyzer.getMeta(entityManager, applicationContext);
        System.out.println(meta.keySet().size());
    }

}
