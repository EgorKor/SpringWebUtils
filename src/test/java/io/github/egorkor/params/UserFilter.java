package io.github.egorkor.params;

import io.github.egorkor.model.User;
import io.github.egorkor.webutils.annotations.AllowedOperations;
import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountLimit;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.filterInternal.FilterOperation;

@ParamCountLimit(2)
public class UserFilter extends Filter<User> {
    //С клиента принимает как filter=orders_name:like:order1
    //Маппится в orders.name
    @ParamCountLimit(1)
    @AllowedOperations({FilterOperation.CONTAINS, FilterOperation.NOT_CONTAINS, FilterOperation.LIKE})
    @FieldParamMapping(requestParamMapping = "orders_name", sqlMapping = "orders.name")
    private String orderNameLike;
    private Long id;
}
