package io.github.egorkor.params;

import io.github.egorkor.webutils.annotations.ParamCountLimit;
import io.github.egorkor.webutils.queryparam.Sorting;

@ParamCountLimit(1)
public class UserSort extends Sorting {
    private Long id;
    private String name;
}
