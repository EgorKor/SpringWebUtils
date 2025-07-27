package io.github.egorkor.webutils.analyze;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
    //определяется в зависимости от типа поля
    private String type;
    private List<Object> validators;
    private List<Object> choices;
}
