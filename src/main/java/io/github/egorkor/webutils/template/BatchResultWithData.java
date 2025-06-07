package io.github.egorkor.webutils.template;

public interface BatchResultWithData<T> extends BatchResult {
    T getData();
}
