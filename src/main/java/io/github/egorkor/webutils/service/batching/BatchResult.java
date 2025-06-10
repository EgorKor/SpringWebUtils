package io.github.egorkor.webutils.service.batching;

public interface BatchResult {
    String getMessage();

    String getDetails();

    BatchOperationStatus getStatus();
}
