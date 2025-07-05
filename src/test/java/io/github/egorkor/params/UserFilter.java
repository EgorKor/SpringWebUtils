package io.github.egorkor.params;

import io.github.egorkor.model.User;
import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountConstraint;
import io.github.egorkor.webutils.queryparam.Filter;

@ParamCountConstraint(1)
public class UserFilter extends Filter<User> {
    //С клиента принимает как filter=orders_name:like:order1
    //Маппится в orders.name
    @FieldParamMapping(requestParamMapping = "orders_name", sqlMapping = "orders.name")
    private String orderNameLike;
}
