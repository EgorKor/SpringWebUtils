package io.github.egorkor.webutils.service.batching;


/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public interface BatchResultWithData<T> extends BatchResult {
    /**
     * Результат пакетной операции
     */
    T getData();
}
