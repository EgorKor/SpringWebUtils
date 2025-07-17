package io.github.egorkor.tests.utils;

import io.github.egorkor.webutils.annotations.FieldParamMapping;
import io.github.egorkor.webutils.annotations.ParamCountLimit;
import io.github.egorkor.webutils.queryparam.utils.ParamValidationUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParamValidationUtilsTest {

    @Test
    void testValidateAllowedParamsWithinLimit() {
        // Test class with annotations
        @ParamCountLimit(value = 3)
        class TestParams {
            @FieldParamMapping(requestParamMapping = "name")
            String username;
            @FieldParamMapping(requestParamMapping = "age")
            int userAge;
            String status;
        }

        List<String> params = Arrays.asList("name=John", "age=30");
        assertDoesNotThrow(() -> ParamValidationUtils.validateAllowedParams(
                params,
                TestParams.class,
                ParamValidationUtils.ParamType.FILTER,
                s -> s.split("="),
                Collections.emptyList()
        ));
    }

    @Test
    void testValidateAllowedParamsExceedsLimit() {
        @ParamCountLimit(value = 2)
        class TestParams {
            String field1;
            String field2;
            String field3;
        }

        List<String> params = Arrays.asList("field1=val1", "field2=val2", "field3=val3");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ParamValidationUtils.validateAllowedParams(
                        params,
                        TestParams.class,
                        ParamValidationUtils.ParamType.FILTER,
                        s -> s.split("="),
                        Collections.emptyList()
                ));

        assertTrue(exception.getMessage().contains("Illegal filter param count: 3"));
    }

    @Test
    void testValidateAllowedParamsWithUnlimitedCount() {
        @ParamCountLimit(value = ParamCountLimit.UNLIMITED)
        class TestParams {
            String field1;
            String field2;
        }

        List<String> params = new ArrayList<>(Arrays.asList("field1=val1", "field2=val2"));
        assertDoesNotThrow(() -> ParamValidationUtils.validateAllowedParams(
                params,
                TestParams.class,
                ParamValidationUtils.ParamType.FILTER,
                s -> s.split("="),
                Collections.emptyList()
        ));
    }

    @Test
    void testValidateAllowedParamsWithNonAllowedFields() {
        class TestParams {
            @FieldParamMapping(requestParamMapping = "allowed")
            String allowedField;
        }

        List<String> params = Arrays.asList("allowed=yes", "forbidden=no");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                ParamValidationUtils.validateAllowedParams(
                        params,
                        TestParams.class,
                        ParamValidationUtils.ParamType.SORT,
                        s -> s.split("="),
                        Collections.emptyList()
                ));

        assertTrue(exception.getMessage().contains("Illegal sort params: [forbidden]"));
    }

    @Test
    void testValidateAllowedParamsWithWhitelist() {
        class TestParams {
            String field1;
        }

        List<String> params = Arrays.asList("field1=val1", "whitelisted=val2");
        assertDoesNotThrow(() -> ParamValidationUtils.validateAllowedParams(
                params,
                TestParams.class,
                ParamValidationUtils.ParamType.FILTER,
                s -> s.split("="),
                Collections.singletonList("whitelisted")
        ));
    }

    @Test
    void testValidateAllowedParamsWithDefaultFieldNames() {
        class TestParams {
            String defaultField;
            @FieldParamMapping
            String explicitField;
        }

        List<String> params = Arrays.asList("defaultField=val1", "explicitField=val2");
        assertDoesNotThrow(() -> ParamValidationUtils.validateAllowedParams(
                params,
                TestParams.class,
                ParamValidationUtils.ParamType.FILTER,
                s -> s.split("="),
                Collections.emptyList()
        ));
    }

    @Test
    void testMapParamsByFilter() {
        class TestParams {
            @FieldParamMapping(requestParamMapping = "ui_name", sqlMapping = "db_name")
            String name;
            @FieldParamMapping(sqlMapping = "db_age")
            int age;
            @FieldParamMapping(requestParamMapping = "status", sqlMapping = "db_status")
            String userStatus;
        }

        List<String> params = Arrays.asList("ui_name=John", "age=30", "status=active");
        ParamValidationUtils.mapParamsByFilter(
                params,
                TestParams.class,
                s -> s.split("=")
        );

        assertEquals("db_name=John", params.get(0));
        assertEquals("db_age=30", params.get(1));
        assertEquals("db_status=active", params.get(2));
    }

    @Test
    void testMapParamsByFilterWithNoMapping() {
        class TestParams {
            @FieldParamMapping
            String name;
            int age;
        }

        List<String> params = Arrays.asList("name=John", "age=30");
        List<String> originalParams = List.copyOf(params);
        ParamValidationUtils.mapParamsByFilter(
                params,
                TestParams.class,
                s -> s.split("=")
        );

        assertEquals(originalParams, params);
    }

    @Test
    void testMapParamsByFilterWithComplexNames() {
        class TestParams {
            @FieldParamMapping(requestParamMapping = "user-name", sqlMapping = "user_name")
            String name;
            @FieldParamMapping(requestParamMapping = "user.age", sqlMapping = "user_age")
            int age;
        }

        List<String> params = Arrays.asList("user-name=John", "user.age=30");
        ParamValidationUtils.mapParamsByFilter(
                params,
                TestParams.class,
                s -> s.split("=")
        );

        assertEquals("user_name=John", params.get(0));
        assertEquals("user_age=30", params.get(1));
    }

    @Test
    void testMapParamsByFilterWithNoAnnotation() {
        class TestParams {
            String name;
            int age;
        }

        List<String> params = Arrays.asList("name=John", "age=30");
        List<String> originalParams = List.copyOf(params);
        ParamValidationUtils.mapParamsByFilter(
                params,
                TestParams.class,
                s -> s.split("=")
        );

        assertEquals(originalParams, params);
    }

    @Test
    void testMapParamsByFilterWithMultipleEqualSigns() {
        class TestParams {
            @FieldParamMapping(requestParamMapping = "name", sqlMapping = "full_name")
            String name;
        }

        List<String> params = new ArrayList<>(Collections.singletonList("name=John=Doe"));
        ParamValidationUtils.mapParamsByFilter(
                params,
                TestParams.class,
                s -> new String[]{s.substring(0, s.indexOf("=")), s.substring(s.indexOf("=") + 1)}
        );

        assertEquals("full_name=John=Doe", params.get(0));
    }
}
