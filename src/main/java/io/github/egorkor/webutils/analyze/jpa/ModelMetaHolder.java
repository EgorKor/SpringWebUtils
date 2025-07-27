package io.github.egorkor.webutils.analyze.jpa;

import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ModelMetaHolder {
    private final Map<Class<?>, ModelMeta> meta;
    private final Map<String, Class<?>> catalogNameClassMapping;
    private final Map<String, Class<?>> catalogVerboseNameClassMapping;

    public ModelMetaHolder(Map<Class<?>, ModelMeta> meta) {
        this.meta = meta;
        catalogNameClassMapping = new HashMap<>();
        catalogVerboseNameClassMapping = new HashMap<>();
        for (var entry : meta.entrySet()) {
            catalogNameClassMapping.put(entry.getValue().getName(), entry.getKey());
            catalogVerboseNameClassMapping.put(entry.getValue().getVerboseName(), entry.getKey());
        }

    }

    public ModelMeta getModelMeta(String name) {
        ModelMeta modelMeta = meta.get(catalogNameClassMapping.get(name));
        if(modelMeta == null){
            throw new ResourceNotFoundException("Справочник с именем " + name + " не найден.");
        }
        return modelMeta;
    }

    public List<SimpleModelMeta> searchMeta(String request) {
        request = request.toLowerCase().trim();
        if(request.isEmpty()){
            return new ArrayList<>();
        }
        List<SimpleModelMeta> metas = new ArrayList<>();
        for (var entry : catalogVerboseNameClassMapping.entrySet()) {
            if(entry.getKey().toLowerCase().contains(request)){
                metas.add(new SimpleModelMeta(meta.get(entry.getValue())));
            }
        }
        return metas;
    }

}
