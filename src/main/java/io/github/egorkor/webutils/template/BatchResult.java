package io.github.egorkor.template;

public interface BatchResult {
    String getMessage();

    String getDetails();

    BatchOperationStatus getStatus();
}
