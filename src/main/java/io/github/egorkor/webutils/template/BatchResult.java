package io.github.egorkor.webutils.template;

public interface BatchResult {
    String getMessage();

    String getDetails();

    BatchOperationStatus getStatus();
}
