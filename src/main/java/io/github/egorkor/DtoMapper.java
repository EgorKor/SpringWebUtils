package io.github.egorkor;

import org.modelmapper.ModelMapper;

import java.util.List;

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
