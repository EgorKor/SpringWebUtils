package io.github.egorkor.webutils.analyze.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class ModelMeta {
    private Set<ModelAttributeMeta> attributes;
    private String name;
    private String verboseName;
}
