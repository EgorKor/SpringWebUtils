package io.github.egorkor.tests.analyze;

import io.github.egorkor.webutils.analyze.jpa.ModelMeta;
import io.github.egorkor.webutils.analyze.jpa.ModelMetaHolder;
import io.github.egorkor.webutils.analyze.jpa.SimpleModelMeta;
import io.github.egorkor.webutils.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelMetaHolderTest {

    @Mock
    private ModelMeta userModelMeta;

    @Mock
    private ModelMeta productModelMeta;

    @Test
    void getModelMeta_shouldReturnMeta_whenExists() {
        // Arrange
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        metaMap.put(String.class, userModelMeta);
        metaMap.put(Integer.class, productModelMeta);

        when(userModelMeta.getName()).thenReturn("user");
        when(userModelMeta.getVerboseName()).thenReturn("Пользователь");

        ModelMetaHolder holder = new ModelMetaHolder(metaMap);

        // Act
        ModelMeta result = holder.getModelMeta("user");

        // Assert
        assertNotNull(result);
        assertEquals(userModelMeta, result);
    }

    @Test
    void getModelMeta_shouldThrow_whenNotExists() {
        // Arrange
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        ModelMetaHolder holder = new ModelMetaHolder(metaMap);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> holder.getModelMeta("nonexistent"));
    }

    @Test
    void searchMeta_shouldReturnEmptyList_whenRequestEmpty() {
        // Arrange
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        ModelMetaHolder holder = new ModelMetaHolder(metaMap);

        // Act
        List<SimpleModelMeta> result = holder.searchMeta("");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void searchMeta_shouldFindByVerboseName() {
        // Arrange
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        metaMap.put(String.class, userModelMeta);
        metaMap.put(Integer.class, productModelMeta);

        when(userModelMeta.getName()).thenReturn("user");
        when(userModelMeta.getVerboseName()).thenReturn("Пользователь");
        when(productModelMeta.getName()).thenReturn("product");
        when(productModelMeta.getVerboseName()).thenReturn("Продукт");

        ModelMetaHolder holder = new ModelMetaHolder(metaMap);

        // Act - поиск по части названия (регистронезависимый)
        List<SimpleModelMeta> result = holder.searchMeta("польз");

        // Assert
        assertEquals(1, result.size());
        assertEquals("user", result.get(0).getName());
    }

    @Test
    void searchMeta_shouldReturnMultipleResults() {
        // Arrange
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        metaMap.put(String.class, userModelMeta);
        metaMap.put(Integer.class, productModelMeta);

        when(userModelMeta.getName()).thenReturn("user");
        when(userModelMeta.getVerboseName()).thenReturn("Пользователь системы");
        when(productModelMeta.getName()).thenReturn("product");
        when(productModelMeta.getVerboseName()).thenReturn("Продукт системы");

        ModelMetaHolder holder = new ModelMetaHolder(metaMap);

        // Act - поиск по общему слову
        List<SimpleModelMeta> result = holder.searchMeta("системы");

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void constructor_shouldBuildMappingsCorrectly() {
        // Arrange
        Map<Class<?>, ModelMeta> metaMap = new HashMap<>();
        metaMap.put(String.class, userModelMeta);

        when(userModelMeta.getName()).thenReturn("user");
        when(userModelMeta.getVerboseName()).thenReturn("Пользователь");

        // Act
        ModelMetaHolder holder = new ModelMetaHolder(metaMap);

        // Assert
        assertEquals(String.class, holder.getCatalogNameClassMapping().get("user"));
        assertEquals(String.class, holder.getCatalogVerboseNameClassMapping().get("Пользователь"));
    }
}
