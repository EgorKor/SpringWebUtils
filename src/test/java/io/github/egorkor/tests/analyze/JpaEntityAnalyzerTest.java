package io.github.egorkor.tests.analyze;

import io.github.egorkor.webutils.analyze.jpa.JpaCatalogEntityMetaAnalyzer;
import io.github.egorkor.webutils.analyze.jpa.ModelMeta;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Map;

@DataJpaTest
public class JpaEntityAnalyzerTest {
    @Autowired
    private EntityManager entityManager;



    @Test
    public void test1(){
        Map<Class<?>, ModelMeta> meta = JpaCatalogEntityMetaAnalyzer.getMeta(entityManager);
        JpaCatalogEntityMetaAnalyzer.getMeta(entityManager);
        System.out.println(meta.keySet().size());

    }




}
