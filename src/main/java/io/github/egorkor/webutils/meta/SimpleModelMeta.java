package io.github.egorkor.webutils.meta;

import lombok.Getter;

@Getter
public class SimpleModelMeta {
    private final String name;
    private final String url;
    private final String verboseName;

    public SimpleModelMeta(ModelMeta meta) {
        this.name = meta.getName();
        this.verboseName = meta.getVerboseName();
        this.url = meta.getUrl();
    }
}
