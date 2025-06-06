package io.github.egorkor.template;

public interface BatchResultWithData<T> extends BatchResult {
    T getData();
}
