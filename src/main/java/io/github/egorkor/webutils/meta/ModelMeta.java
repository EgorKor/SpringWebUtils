package io.github.egorkor.webutils.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class ModelMeta {
    private String name;
    private String url;
    private String verboseName;
    private List<ModelAttributeMeta> attributes;

    @JsonIgnore
    public ModelMeta getMetaWithAttributeChoices() {
        return ModelMeta.builder()
                .name(name)
                .verboseName(verboseName)
                .attributes(attributes.stream()
                        .map(ModelAttributeMeta::getWithChoices)
                        .toList())
                .build();
    }

    @Override
    public String toString() {
        return "ModelMeta(name=" + this.getName()
                + ", verboseName=" + this.getVerboseName()
                + "\nattributes=" + collectionToString(attributes) + ")";
    }

    private String collectionToString(Collection<?> set) {
        StringBuilder sb = new StringBuilder();
        set.forEach(el -> sb.append(el.toString()).append("\n"));
        return sb.toString();
    }
}
