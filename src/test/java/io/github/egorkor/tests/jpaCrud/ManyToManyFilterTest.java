package io.github.egorkor.tests.jpaCrud;

import io.github.egorkor.model.EducationProgram;
import io.github.egorkor.service.EducationProgramService;
import io.github.egorkor.service.impl.EducationProgramServiceImpl;
import io.github.egorkor.service.impl.OrderServiceImpl;
import io.github.egorkor.service.impl.UserServiceImpl;
import io.github.egorkor.webutils.queryparam.Filter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

@Import({OrderServiceImpl.class, UserServiceImpl.class, LocalValidatorFactoryBean.class, EducationProgramServiceImpl.class})
@ActiveProfiles("test")
@DataJpaTest
public class ManyToManyFilterTest {
    @Autowired
    private EntityManager em;
    @Autowired
    private EducationProgramService educationProgramService;

    @Test
    public void test(){
        /*CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<EducationProgram> q = cb.createQuery(EducationProgram.class);
        Root<EducationProgram> root = q.from(EducationProgram.class);

        q.where(cb.equal(root.get("profiles").get("name"), "234"));
        TypedQuery<EducationProgram> tq = em.createQuery(q);
        List<EducationProgram> list = tq.getResultList();*/

        educationProgramService.getList(
                Filter.equals("profiles.name","234")
        );


    }

}
