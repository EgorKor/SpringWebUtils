package io.github.egorkor.webutils.service.batching;

public interface BatchResultWithData<T> extends BatchResult {
    /** Результат пакетной операции */
    T getData();
}
