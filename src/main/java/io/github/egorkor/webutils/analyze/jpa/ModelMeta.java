package io.github.egorkor.webutils.analyze.jpa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.stream.Collectors;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class ModelMeta {
    private String name;
    private String verboseName;
    private Set<ModelAttributeMeta> attributes;

    public ModelMeta getMetaWithAttributeChoices(){
        return ModelMeta.builder()
                .name(name)
                .verboseName(verboseName)
                .attributes(attributes.stream()
                        .map(ModelAttributeMeta::getWithChoices)
                        .collect(Collectors.toSet()))
                .build();
    }
}
