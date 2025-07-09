package io.github.egorkor.webutils.service.sync;

import io.github.egorkor.webutils.exception.EntityProcessingException;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import io.github.egorkor.webutils.exception.SoftDeleteUnsupportedException;
import io.github.egorkor.webutils.queryparam.Filter;
import io.github.egorkor.webutils.queryparam.PageableResult;
import io.github.egorkor.webutils.queryparam.Pagination;
import io.github.egorkor.webutils.queryparam.Sorting;
import jakarta.persistence.LockModeType;

/**
 * Интерфейс базового CRUD параметризованного сервиса
 * <p>
 * Методы
 *     <ul>
 *         <li>{@link #getAll(Filter, Sorting, Pagination)}</li>
 *         <li>{@link #getById(ID)}</li>
 *         <li>{@link #getByIdWithLock(ID, LockModeType)}</li>
 *         <li>{@link #getByFilter(Filter)}</li>
 *         <li>{@link #getByFilterWithLock(Filter, LockModeType)}</li>
 *         <li>{@link #create(T)}</li>
 *         <li>{@link #fullUpdate(T)}</li>
 *         <li>{@link #patchUpdate(ID, T)}</li>
 *         <li>{@link #deleteAll()}</li>
 *         <li>{@link #deleteById(ID)}</li>
 *         <li>{@link #deleteByFilter(Filter)}</li>
 *         <li>{@link #countAll()}</li>
 *         <li>{@link #countByFilter(Filter)}</li>
 *         <li>{@link #existsById(ID)}</li>
 *         <li>{@link #existsByFilter(Filter)}</li>
 *         <li>{@link #softDeleteById(ID)}</li>
 *         <li>{@link #softDeleteByFilter(Filter)}</li>
 *         <li>{@link #softDeleteAll()}</li>
 *         <li>{@link #restoreById(ID)}</li>
 *         <li>{@link #restoreByFilter(Filter)}</li>
 *         <li>{@link #restoreAll()}</li>
 *     </ul>
 * </p>
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public interface CrudService<T, ID> {
    /**
     * Запрос на получение списка сущностей с учётом фильтрации, сортировки, пагинации
     *
     * @param sorting    параметр запроса сортировки
     * @param filter     параметр запроса фильтрации
     * @param pagination параметр запроса постраничного доступа
     * @return PageableResult - результат постраничного запроса к БД, содержащий данные
     * и параметры страниц
     */
    PageableResult<T> getAll(Filter<T> filter, Sorting sorting, Pagination pagination);

    /**
     * Запрос на получение сущности по идентификатору
     *
     * @param id идентификатор сущности
     * @return объект T - сущность найденная по id
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с таким id
     */
    T getById(ID id) throws ResourceNotFoundException;

    /**
     * Запрос на получение сущности по идентификатору
     *
     * @param id идентификатор сущности
     * @return объект T - сущность найденная по id
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с таким id
     */
    T getByIdWithFilter(ID id, Filter<T> filter) throws ResourceNotFoundException;

    /**
     * Запрос на получение сущности по идентификатору с возможностью блокировки
     * записи на уровне базы данных
     *
     * @param id       идентификатор сущности
     * @param lockType параметр блокировки в БД
     * @return объект T - сущность найденная по id
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с таким id
     */
    T getByIdWithLock(ID id, LockModeType lockType) throws ResourceNotFoundException;

    /**
     * Запрос на получение одной сущности с применением условий из фильтра
     *
     * @param filter параметр запроса фильтрации
     * @return объект Т - удовлетворяющий условиям из фильтра
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности удовлетворяющий
     *                                   условиям из фильтра
     */
    T getByFilter(Filter<T> filter) throws ResourceNotFoundException;

    /**
     * Запрос на получение одной сущности с применением условий из фильтра
     * с возможностью блокировки записи на уровне базы данных
     *
     * @param filter   параметр запроса фильтрации
     * @param lockType параметр блокировки в БД
     * @return объект Т - удовлетворяющий условиям из фильтра
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности удовлетворяющий
     *                                   условиям из фильтра
     */
    T getByFilterWithLock(Filter<T> filter, LockModeType lockType) throws ResourceNotFoundException;

    /**
     * Создание (POST) сущности в БД
     *
     * @return объект сущности после сохранения в БД - модифицированный
     */
    T create(T model) throws EntityProcessingException;

    /**
     * Полное (PUT) обновление сущности на основе переданной модели, переписывает все поля оригинальной сущности
     *
     * @return объект сущности после обновления в БД
     */
    T fullUpdate(T model) throws EntityProcessingException;

    /**
     * Частичное (PATCH) обновление сущности на основе переданной модели, обновляет только не null
     * поля, которые отличаются от оригинальных.
     *
     * @param id    идентификатор сущности
     * @param model объект сущности
     * @return объект сущности после обновления в БД
     * @throws ResourceNotFoundException в случае отсутствия в БД сущности с указанным id
     */
    T patchUpdate(ID id, T model) throws ResourceNotFoundException, EntityProcessingException;

    /**
     * Физическое удаление сущности по ID
     *
     * @param id идентификатор сущности
     */
    void deleteById(ID id) throws ResourceNotFoundException, EntityProcessingException;

    /**
     * Физическое удаление всех сущностей
     */
    void deleteAll() throws EntityProcessingException;

    /**
     * Физическое удаление всех сущностей с учётом фильтрации
     *
     * @param filter параметр запроса фильтрации
     */
    void deleteByFilter(Filter<T> filter) throws EntityProcessingException;

    /**
     * Кол-во сущностей с учётом фильтрации
     *
     * @param filter параметр запроса фильтрации
     * @return общее кол-во сущностей в БД удовлетворяющих условиям фильтра
     */
    long countByFilter(Filter<T> filter);

    /**
     * Кол-во сущностей
     *
     * @return общее кол-во сущностей в БД
     */
    long countAll();

    /**
     * Проверка существования сущности по ID
     *
     * @param id идентификатор сущности
     * @return true - если сущность существует с указанным id
     */
    boolean existsById(ID id);

    /**
     * Проверка существования сущности по условию из фильтра
     *
     * @param filter - параметр запроса с фильтрацией
     * @return true - если сущность существует удовлетворяющая условиям фильтра
     */
    boolean existsByFilter(Filter<T> filter);

    /**
     * Мягкое удаление по ID
     *
     * @param id идентификатор сущности
     * @throws ResourceNotFoundException      если сущности с указанным id не существует в БД
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void softDeleteById(ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Мягкое удаление всех сущностей
     *
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void softDeleteAll() throws SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Мягкое удаление всех по условию
     *
     * @param filter параметр запроса
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void softDeleteByFilter(Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Восстановление после мягкого удаления по ID
     *
     * @param id идентификатор сущности
     * @throws ResourceNotFoundException      если сущности с указанным id не существует в БД
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void restoreById(ID id) throws ResourceNotFoundException, SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Восстановление всех записей
     *
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void restoreAll() throws SoftDeleteUnsupportedException, EntityProcessingException;

    /**
     * Восстановление всех записей с учётом условий фильтрации
     *
     * @throws SoftDeleteUnsupportedException если сущность не поддерживает мягкое удаление
     */
    void restoreByFilter(Filter<T> filter) throws SoftDeleteUnsupportedException, EntityProcessingException;
}
