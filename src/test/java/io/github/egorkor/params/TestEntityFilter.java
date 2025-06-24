package io.github.egorkor.params;

import io.github.egorkor.model.TestEntity;
import io.github.egorkor.webutils.annotations.FieldAllies;
import io.github.egorkor.webutils.queryparam.Filter;

public class TestEntityFilter extends Filter<TestEntity> {
    private Long id;
    @FieldAllies("_name")
    private String filterName;
}
