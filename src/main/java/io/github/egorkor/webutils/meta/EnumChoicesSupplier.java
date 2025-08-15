package io.github.egorkor.webutils.meta;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class EnumChoicesSupplier implements Supplier<List<Object>> {
    private final List<Object> cachedEnumChoices;

    @Override
    public List<Object> get() {
        return cachedEnumChoices;
    }
}
