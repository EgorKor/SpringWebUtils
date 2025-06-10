package io.github.egorkor.webutils.service.batching;

public interface BatchResultWithData<T> extends BatchResult {
    T getData();
}
