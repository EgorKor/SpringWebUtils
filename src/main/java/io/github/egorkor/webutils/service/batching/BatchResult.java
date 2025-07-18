package io.github.egorkor.webutils.service.batching;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public interface BatchResult {
    /**
     * Сообщение характеризующее результат пакетной операции.
     * Пример: "updated"
     * Пример: "update operation failed for entity: User(id=1,name="Name")"
     */
    String getMessage();

    /**
     * Детали ошибки пакетной операции
     * Пример: exception message
     */
    String getDetails();

    /**
     * Статус операции - SUCCESS или FAILED
     */
    BatchOperationStatus getStatus();
}
