package io.github.egorkor.webutils.analyze.jpa;

import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import io.github.egorkor.webutils.annotations.Choices;
import io.github.egorkor.webutils.annotations.SoftDeleteFlag;
import io.github.egorkor.webutils.queryparam.Filter;
import jakarta.persistence.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.validation.constraints.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Range;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class JpaCatalogEntityMetaAnalyzer {
    private final static List<Predicate<Field>> FIELD_META_PREDICATES = new ArrayList<>();
    private final static Map<EntityManager, Map<Class<?>, ModelMeta>> CACHE = new HashMap<>();

    static {
        //IGNORE TOOL FIELDS
        FIELD_META_PREDICATES.add(field -> field.getAnnotation(SoftDeleteFlag.class) == null
                && field.getAnnotation(OneToMany.class) == null
                && field.getAnnotation(ManyToMany.class) == null
                && field.getAnnotation(CreationTimestamp.class) == null
                && field.getAnnotation(UpdateTimestamp.class) == null
                && field.getAnnotation(CreatedBy.class) == null
                && field.getAnnotation(CreatedDate.class) == null
                && field.getAnnotation(Version.class) == null
                && field.getAnnotation(org.springframework.data.annotation.Version.class) == null
                && field.getAnnotation(LastModifiedBy.class) == null
                && field.getAnnotation(LastModifiedDate.class) == null);
    }


    public static Map<Class<?>, ModelMeta> getMeta(@NonNull EntityManager entityManager,
                                                   @NonNull ApplicationContext context) {
        if (CACHE.containsKey(entityManager)) {
            return CACHE.get(entityManager);
        }
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();
        for (var entityType : entityTypes) {
            if (entityType.getJavaType().getAnnotation(CatalogMeta.class) == null) {
                continue;
            }
            metaMap.put(entityType.getJavaType(), getModelMetaForEntityType(entityType, context));
        }
        CACHE.put(entityManager, metaMap);
        return metaMap;
    }

    public static ModelMeta getModelMetaForEntityType(@NonNull EntityType<?> entityType,
                                                      @NonNull ApplicationContext context) {
        CatalogMeta catalogMeta;
        if ((catalogMeta = entityType.getJavaType().getAnnotation(CatalogMeta.class)) == null) {
            throw new IllegalStateException("Entity should be annotated with @CatalogMeta to get ModelMeta");
        }
        String verboseName = catalogMeta.verboseName() != null ? catalogMeta.verboseName() : entityType.getJavaType().getSimpleName();
        Set<ModelAttributeMeta> modelAttributeMetaSet = new HashSet<>();
        Set<Attribute<?, ?>> attributes = (Set<Attribute<?, ?>>) entityType.getAttributes();
        attributeMetaCycle:
        for (var attribute : attributes) {
            try {
                Field field = ReflectionUtils.findField(
                        entityType.getJavaType(), attribute.getName()
                );
                if (field == null) {
                    continue;
                }
                field.setAccessible(true);
                for (Predicate<Field> predicate : FIELD_META_PREDICATES) {
                    if (!predicate.test(field)) {
                        continue attributeMetaCycle;
                    }
                }
                modelAttributeMetaSet.add(getModelAttributeMetaForField(field, context));
            } catch (Exception e) {
                log.debug("Error while getting meta attributes for entity {} {}", entityType.getJavaType().getSimpleName(), attribute.getName(), e);
            }
        }

        return ModelMeta.builder()
                .name(entityType.getName())
                .attributes(modelAttributeMetaSet)
                .verboseName(verboseName)
                .build();
    }

    public static ModelAttributeMeta getModelAttributeMetaForField(@NonNull Field field,
                                                                   @NonNull ApplicationContext applicationContext) {
        String name = field.getName();
        String verboseName = name;
        boolean isRelation = false;
        boolean required = false;
        String type;
        String relatedEntity = null;
        String placeholder = "";
        //define verbose_name and
        if (field.isAnnotationPresent(AttributeMeta.class)) {
            AttributeMeta attributeMeta = field.getAnnotation(AttributeMeta.class);
            verboseName = attributeMeta.verboseName();
            required = attributeMeta.required();
            placeholder = attributeMeta.placeholder();
        }
        //define is_relation
        {
            isRelation = field.isAnnotationPresent(OneToOne.class) || field.isAnnotationPresent(ManyToOne.class);
            if (isRelation) {
                Class<?> fieldType = field.getType();
                if (Collection.class.isAssignableFrom(fieldType)) {
                    fieldType = Filter.getCollectionElementType(field);
                }
                relatedEntity = fieldType.getSimpleName();
            }
        }
        //define type
        {
            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType)) {
                type = "LIST";
                if (!isRelation) {
                    fieldType = Filter.getCollectionElementType(field);
                    type += " " + fieldType.getSimpleName();
                }
            } else {
                if (Double.class.isAssignableFrom(fieldType)
                        || double.class.isAssignableFrom(fieldType)
                        || Float.class.isAssignableFrom(fieldType)
                        || float.class.isAssignableFrom(fieldType)) {
                    type = "FLOAT";
                } else if (Integer.class.isAssignableFrom(fieldType)
                        || int.class.isAssignableFrom(fieldType)
                        || Long.class.isAssignableFrom(fieldType)
                        || long.class.isAssignableFrom(fieldType)
                        || Short.class.isAssignableFrom(fieldType)
                        || short.class.isAssignableFrom(fieldType)
                        || Byte.class.isAssignableFrom(fieldType)
                        || byte.class.isAssignableFrom(fieldType)) {
                    type = "INT";
                } else if (String.class.isAssignableFrom(fieldType)) {
                    type = "STRING";
                } else if (fieldType.isEnum()) {
                    type = "ENUM";
                } else if (boolean.class.isAssignableFrom(fieldType)
                        || Boolean.class.isAssignableFrom(fieldType)) {
                    type = "BOOLEAN";
                } else {
                    type = "OBJECT";
                }
            }
            if (field.isAnnotationPresent(GeneratedValue.class)) {
                type = "GENERATED " + type;
            }
        }
        Supplier<List<Object>> choicesSupplier = null;
        //define_validators
        List<Validator> validators = getValidatorsForField(field);
        if (type.equals("ENUM") || isRelation) {
            choicesSupplier = getChoicesSupplierForField(field, applicationContext, type);
        }

        return ModelAttributeMeta.builder()
                .type(type)
                .name(name)
                .required(required)
                .relatedModel(relatedEntity)
                .isRelation(isRelation)
                .validators(validators)
                .verboseName(verboseName)
                .placeholder(placeholder)
                .choicesSupplier(choicesSupplier)
                .build();
    }

    public static Supplier<List<Object>> getChoicesSupplierForField(@NonNull Field field,
                                                                    @NonNull ApplicationContext applicationContext,
                                                                    @NonNull String type) {
        if (!field.isAnnotationPresent(Choices.class)) {
            throw new IllegalStateException("Field should be annotated with @Choices to get Choices");
        }
        Supplier<List<Object>> choicesSupplier = List::of;

        switch (type) {
            case "ENUM" -> {
                Class<?> enumType = field.getType();
                Object[] constants = enumType.getEnumConstants();
                Field verboseNameField = ReflectionUtils.findField(enumType, "verboseName");
                boolean hasVerboseName = verboseNameField != null;
                if (hasVerboseName) {
                    verboseNameField.setAccessible(true);
                }
                return new EnumChoicesSupplier(Arrays.stream(constants)
                        .map(obj -> (Enum) obj)
                        .map(constant -> {
                            try {
                                String verboseName;
                                if (hasVerboseName) {
                                    Object verboseNameObject = verboseNameField.get(constant);
                                    verboseName = verboseNameObject == null ? constant.name() : verboseNameObject.toString();
                                } else {
                                    verboseName = constant.name();
                                }
                                return (Object) new EnumChoice(constant.name(), verboseName);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException("Cannot access ENUM field 'verboseName' in class " + enumType.getSimpleName(), e);
                            }
                        })
                        .toList());
            }
            case "OBJECT" -> {
                Object choiceSupplierBean = applicationContext.getBean(field.getAnnotation(Choices.class).value());
                if(choiceSupplierBean instanceof ChoicesSupplier supplier){
                    return supplier::getChoices;
                }else{
                    throw new IllegalStateException("@Choices class should implements ChoicesSupplier interface or be an Enum type - " + choiceSupplierBean);
                }
            }
            default -> {
                return choicesSupplier;
            }
        }
    }

    public static List<Validator> getValidatorsForField(Field field) {
        List<Validator> validators = new ArrayList<>();
        if (field.isAnnotationPresent(NotNull.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.NOT_NULL)
                    .build());
        }
        if (field.isAnnotationPresent(NotEmpty.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.NOT_EMPTY)
                    .build());
        }
        if (field.isAnnotationPresent(NotBlank.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.NOT_BLANK)
                    .build());
        }
        if (field.isAnnotationPresent(Range.class)) {
            Range range = field.getAnnotation(Range.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.NOT_BLANK)
                    .constraints(Map.of("minValue", range.min(), "maxValue", range.max()))
                    .build());
        }
        if (field.isAnnotationPresent(Size.class)) {
            Size size = field.getAnnotation(Size.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.SIZE)
                    .constraints(Map.of("minValue", size.min(), "maxValue", size.max()))
                    .build());
        }
        if (field.isAnnotationPresent(Min.class)) {
            Min min = field.getAnnotation(Min.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.MIN)
                    .constraints(Map.of("minValue", min.value()))
                    .build());
        }
        if (field.isAnnotationPresent(Max.class)) {
            Max max = field.getAnnotation(Max.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.MAX)
                    .constraints(Map.of("maxValue", max.value()))
                    .build());
        }
        if (field.isAnnotationPresent(Pattern.class)) {
            Pattern pattern = field.getAnnotation(Pattern.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.PATTERN)
                    .constraints(Map.of("regex", pattern.regexp()))
                    .build());
        }
        if (field.isAnnotationPresent(Email.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.EMAIL)
                    .build());
        }
        if (field.isAnnotationPresent(Negative.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.NEGATIVE)
                    .build());
        }
        if (field.isAnnotationPresent(NegativeOrZero.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.NEGATIVE_OR_ZERO)
                    .build());
        }
        if (field.isAnnotationPresent(Positive.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.POSITIVE)
                    .build());
        }
        if (field.isAnnotationPresent(PositiveOrZero.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.POSITIVE_OR_ZERO)
                    .build());
        }
        if (field.isAnnotationPresent(Future.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.FUTURE)
                    .build());
        }
        if (field.isAnnotationPresent(FutureOrPresent.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.FUTURE_OR_PRESENT)
                    .build());
        }
        if (field.isAnnotationPresent(Past.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.PAST)
                    .build());
        }
        if (field.isAnnotationPresent(PastOrPresent.class)) {
            validators.add(Validator.builder()
                    .validatorCode(ValidatorCode.PAST_OR_PRESENT)
                    .build());
        }
        if (field.isAnnotationPresent(Digits.class)) {
            Digits digits = field.getAnnotation(Digits.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.NEGATIVE_OR_ZERO)
                    .constraints(Map.of("integer", digits.integer(), "fraction", digits.fraction()))
                    .build());
        }
        if (field.isAnnotationPresent(DecimalMin.class)) {
            DecimalMin decimalMin = field.getAnnotation(DecimalMin.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.NEGATIVE_OR_ZERO)
                    .constraints(Map.of("minValue", decimalMin.value()))
                    .build());
        }
        if (field.isAnnotationPresent(DecimalMax.class)) {
            DecimalMax decimalMax = field.getAnnotation(DecimalMax.class);
            validators.add(ValuableValidator.builder()
                    .validatorCode(ValidatorCode.NEGATIVE_OR_ZERO)
                    .constraints(Map.of("maxValue", decimalMax.value()))
                    .build());
        }
        return validators;
    }
}
