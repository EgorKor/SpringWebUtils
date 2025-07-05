package io.github.egorkor.params;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.queryparam.Filter;

public class TestEntityFilter extends Filter<TestEntity> {
    private Long id;
    @FieldParamMapping(requestParamMapping = "name", sqlMapping = "_name")
    private String clientParamName;
}
