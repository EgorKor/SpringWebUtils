package io.github.egorkor.webutils.analyze;

import io.github.egorkor.webutils.annotations.AttributeMeta;
import io.github.egorkor.webutils.annotations.CatalogMeta;
import io.github.egorkor.webutils.annotations.RelationMeta;
import io.github.egorkor.webutils.queryparam.Filter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
@RequiredArgsConstructor
public class JpaCatalogEntityMetaAnalyzer {
    private final EntityManager entityManager;
    private final static List<Predicate<Field>> FIELD_META_PREDICATES = new ArrayList<>();

    static {
        //IGNORE  @OneToMany fields
        //TAKE IF @MetaRelation presents
        FIELD_META_PREDICATES.add(field -> {
            if (field.getAnnotation(OneToMany.class) == null) {
                return true;
            }
            return field.getAnnotation(RelationMeta.class) != null;
        });
        //IGNORE TOOL FIELDS
        FIELD_META_PREDICATES.add(field -> {
            return field.getAnnotation(SoftDelete.class) == null
                    && field.getAnnotation(CreationTimestamp.class) == null
                    && field.getAnnotation(UpdateTimestamp.class) == null
                    && field.getAnnotation(CreatedBy.class) == null
                    && field.getAnnotation(CreatedDate.class) == null
                    && field.getAnnotation(Version.class) == null
                    && field.getAnnotation(org.springframework.data.annotation.Version.class) == null
                    && field.getAnnotation(LastModifiedBy.class) == null
                    && field.getAnnotation(LastModifiedDate.class) == null;
        });

    }


    public Map<Class<?>, ModelMeta> getMeta() {
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        Set<EntityType<?>> entityTypes = entityManager.getMetamodel().getEntities();
        for (var entityType : entityTypes) {
            if (entityType.getJavaType().getAnnotation(CatalogMeta.class) == null) {
                continue;
            }
            metaMap.put(entityType.getJavaType(), getMetaForEntityType(entityType));
        }
        return metaMap;
    }

    public ModelMeta getMetaForEntityType(@NonNull EntityType<?> entityType) {
        CatalogMeta catalogMeta;
        if ((catalogMeta = entityType.getJavaType().getAnnotation(CatalogMeta.class)) == null) {
            throw new IllegalStateException();
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
                modelAttributeMetaSet.add(getMetaForAttribute(field));
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

    public ModelAttributeMeta getMetaForAttribute(@NonNull Field field) {
        String name = field.getName();
        String verboseName = name;
        boolean isRelation = false;
        boolean required = false;
        String type;
        String relatedEntity = null;

        if (field.isAnnotationPresent(AttributeMeta.class)) {
            AttributeMeta attributeMeta = field.getAnnotation(AttributeMeta.class);
            verboseName = attributeMeta.verboseName();
            required = attributeMeta.required();
            isRelation = attributeMeta.isRelation();
        }

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
            if(field.isAnnotationPresent(GeneratedValue.class)) {
                type = "GENERATED " + type;
            }
        }
        if (isRelation) {
            Class<?> fieldType = field.getType();
            if (Collection.class.isAssignableFrom(fieldType)) {
                fieldType = Filter.getCollectionElementType(field);
            }
            relatedEntity = fieldType.getSimpleName();
        }


        return ModelAttributeMeta.builder()
                .type(type)
                .name(name)
                .required(required)
                .relatedModel(relatedEntity)
                .isRelation(isRelation)
                .verboseName(verboseName)
                .build();
    }
}
