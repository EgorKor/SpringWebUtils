package io.github.egorkor.webutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация предназначена для пометки
 * поля JPA сущности, которые является признаком мягкого удаления.
 * Данная аннотация распознается в реализации {@link io.github.egorkor.webutils.template.jpa.JpaCrudService}
 * и включает поддержку мягкого удаления для сущности. Иначе при вызовах методов
 * мягкого удаления и восстановления сущностей
 * будет выброшено исключение {@link io.github.egorkor.webutils.exception.SoftDeleteUnsupportedException}
 * <p>
 * Поддерживаемые типы полей:
 *     <ul>
 *         <li>{@link Boolean}</li>
 *         <li>{@link java.sql.Timestamp}</li>
 *         <li>{@link java.sql.Date}</li>
 *         <li>{@link java.time.Instant}</li>
 *         <li>{@link java.time.LocalDateTime}</li>
 *         <li>{@link java.time.OffsetDateTime}</li>
 *     </ul>
 * </p>
 * <p>Пример использование с датой</p>
 * <pre>
 *     {@code
 *     @Entity
 *     public class TestNestedEntity {
 *          @Id
 *          private Long id;
 *          @ManyToOne
 *          private TestEntity parent;
 *          @SoftDeleteFlag
 *          private LocalDateTime deletedAt;
 *      }}
 * </pre>
 * <p>Пример использования c Boolean флагом</p>
 * <pre>
 *     {@code
 *      @Entity
 *      public class TestEntity {
 *          @Id
 *          private Long id;
 *          private String name;
 *          @OneToMany
 *          private List<TestNestedEntity> nested;
 *          @SoftDeleteFlag
 *          private Boolean isDeleted;
 *      }}
 * </pre>
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SoftDeleteFlag {
}
