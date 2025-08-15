package io.github.egorkor.webutils.meta;

import java.util.List;

@FunctionalInterface
public interface ChoicesSupplier {
    List<Object> getChoices();
}
