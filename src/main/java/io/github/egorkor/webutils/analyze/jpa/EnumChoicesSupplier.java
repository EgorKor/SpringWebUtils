package io.github.egorkor.webutils.analyze.jpa;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class EnumChoicesSupplier implements Supplier<List<Object>> {
    private final List<Object> cachedEnumConstants;

    @Override
    public List<Object> get() {
        return cachedEnumConstants;
    }
}
