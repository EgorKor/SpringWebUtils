package io.github.egorkor.service;

import io.github.egorkor.model.TestingEntityBatching;
import io.github.egorkor.webutils.service.sync.CrudBatchService;

public interface BatchTestEntityService extends CrudBatchService<TestingEntityBatching, Long> {
}
