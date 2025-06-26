package io.github.egorkor.webutils.dto;

import org.modelmapper.ModelMapper;

import java.util.List;

/**
 * DtoMapper - универсальный маппер для маппинга DTO внутри контроллеров.
 * Класс предоставляет удобную читаемую обёртку над классом ModelMapper.
 * Мапинг основан на рефлексии, поэтому для маппинга досточно передать
 * объект или список объектов, и класс в который необходимо преобразовать объект.
 * <pre>
 * {@code
 * User user = userService.getById(id);
 * UserDto dto = mapper.toDto(model, UserDto.class);
 * }
 * </pre>
 * <p>
 * В функциональность маппера также входит маппинг
 * вложенных объектов. Например, маппер может распознать
 * вложенные сущности JPA и при преобразовании в DTO преобразовать
 * их в поле.
 *
 * <pre>
 * {@code
 *     class Node{
 *         @Id
 *         private Long id;
 *         @ManyToOne
 *         private Node parent;
 *         @OneToMany
 *         private List<Node> children;
 *     }
 *
 *     class NodeDto{
 *        private Long id;
 *        private Long parentId;// будет преобразовано из parent -> id
 *     }
 * }
 * </pre>
 *
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
public class DtoMapper {
    private final ModelMapper modelMapper = new ModelMapper();

    public <M, D> D toDto(M model, Class<D> destination) {
        return modelMapper.map(model, destination);
    }

    public <M, D> List<D> toDto(List<M> models, Class<D> destination) {
        return models.stream().map(o -> modelMapper.map(o, destination)).toList();
    }

    public <M, D> M toModel(D dto, Class<M> destination) {
        return modelMapper.map(dto, destination);
    }
}
