package io.github.egorkor.params;

import io.github.egorkor.model.User;
import io.github.egorkor.webutils.annotations.FilterFieldAllies;
import io.github.egorkor.webutils.queryparam.Filter;

public class UserFilter extends Filter<User> {
    @FilterFieldAllies("orders.name")
    private String orderNameLike;
}
