package io.github.egorkor.webutils.analyze.jpa;

import java.util.List;

@FunctionalInterface
public interface ChoicesSupplier {
    List<Object> getChoices();
}
