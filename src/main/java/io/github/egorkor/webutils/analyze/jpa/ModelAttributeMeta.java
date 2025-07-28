package io.github.egorkor.webutils.analyze.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Supplier;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ModelAttributeMeta {
    //определяется по полю сущности
    private String name;
    //задаётся в виде альяса в аннотации @MetaAttribute
    private String verboseName;
    //задаётся в виде параметра в аннотации @MetaAttribute
    private boolean isRelation;
    //определяется по типу поля если isRelation = true
    private String relatedModel;
    //задаётся в виде параметра в аннотации @MetaAttribute
    private boolean required;
    //задаётся в виде параметра в аннотации @MetaAttribute
    private String placeholder;
    //определяется в зависимости от типа поля
    private String type;
    //определяется по аннотациям из пакета jakarta.validation
    private List<Validator> validators;
    //определяется по аннотации @Choices
    private List<Object> choices;
    //поставщик выбора
    @JsonIgnore
    private Supplier<List<Object>> choicesSupplier;

    public ModelAttributeMeta getWithChoices() {
        List<Object> choices = null;
        if(choicesSupplier != null){
            choices = choicesSupplier.get();
        }
        return ModelAttributeMeta.builder()
                .name(name)
                .verboseName(verboseName)
                .isRelation(isRelation)
                .relatedModel(relatedModel)
                .required(required)
                .placeholder(placeholder)
                .type(type)
                .validators(validators)
                .choicesSupplier(choicesSupplier)
                .choices(choices)
                .build();
    }

}
