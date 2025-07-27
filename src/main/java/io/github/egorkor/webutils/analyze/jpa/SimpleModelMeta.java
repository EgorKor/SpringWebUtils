package io.github.egorkor.webutils.analyze.jpa;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SimpleModelMeta {
    private final String name;
    private final String verboseName;

    public SimpleModelMeta(ModelMeta meta) {
        this.name = meta.getName();
        this.verboseName = meta.getVerboseName();
    }
}
