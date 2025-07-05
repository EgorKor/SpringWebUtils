package io.github.egorkor.params;

import io.github.egorkor.model.User;
import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountLimit;
import io.github.egorkor.webutils.queryparam.Filter;

@ParamCountLimit(1)
public class UserFilter extends Filter<User> {
    //С клиента принимает как filter=orders_name:like:order1
    //Маппится в orders.name
    @FieldParamMapping(requestParamMapping = "orders_name", sqlMapping = "orders.name")
    private String orderNameLike;
    private Long id;
}
