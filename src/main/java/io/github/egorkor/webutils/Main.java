package io.github.egorkor.webutils;

import io.github.egorkor.webutils.queryparam.Filter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println(Arrays.toString("id:=:1:or:id:=:2".split(":or:")));
        Filter filter = new Filter(List.of("id:=:1:or:id:=:2","name:not_like:name","name:not_in:names2;names1"));
        System.out.println(filter.toSQLFilter());
        String str = "hello";
        System.out.println(str.getClass());
        System.out.println(str.getClass().getName());
        for(Method m : str.getClass().getMethods()) {
            System.out.println(m);
        }
    }
}