package org.example.jsonschemavalidationpoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jsonschemavalidationpoc.dto.SchemaRequest;
import org.example.jsonschemavalidationpoc.dto.ValidationRequest;
import org.example.jsonschemavalidationpoc.dto.ValidationResponse;
import org.example.jsonschemavalidationpoc.entity.JsonSchemaEntity;
import org.example.jsonschemavalidationpoc.repository.JsonSchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchemaService Unit Tests")
class JsonSchemaServiceTest {

    @Mock
    private JsonSchemaRepository repository;

    @Mock
    private AvroConverterService avroConverterService;

    @InjectMocks
    private JsonSchemaService jsonSchemaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jsonSchemaService = new JsonSchemaService(repository, objectMapper, avroConverterService);
    }

    @Test
    @DisplayName("Should save valid JSON schema successfully")
    void shouldSaveValidSchema() {
        // Arrange
        SchemaRequest request = new SchemaRequest();
        request.setType("user");
        request.setVersion("1.0");
        request.setSchemaContent("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");

        when(repository.existsByTypeAndVersion("user", "1.0")).thenReturn(false);
        when(repository.save(any(JsonSchemaEntity.class))).thenAnswer(invocation -> {
            JsonSchemaEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        // Act
        JsonSchemaEntity result = jsonSchemaService.saveSchema(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo("user");
        assertThat(result.getVersion()).isEqualTo("1.0");
        verify(repository).save(any(JsonSchemaEntity.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON schema")
    void shouldThrowExceptionForInvalidSchema() {
        // Arrange
        SchemaRequest request = new SchemaRequest();
        request.setType("user");
        request.setVersion("1.0");
        request.setSchemaContent("not a valid json");

        // Act & Assert
        assertThatThrownBy(() -> jsonSchemaService.saveSchema(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid JSON schema");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for duplicate schema")
    void shouldThrowExceptionForDuplicateSchema() {
        // Arrange
        SchemaRequest request = new SchemaRequest();
        request.setType("user");
        request.setVersion("1.0");
        request.setSchemaContent("{\"type\":\"object\"}");

        when(repository.existsByTypeAndVersion("user", "1.0")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> jsonSchemaService.saveSchema(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should validate JSON successfully against schema")
    void shouldValidateJsonSuccessfully() {
        // Arrange
        JsonSchemaEntity schemaEntity = new JsonSchemaEntity();
        schemaEntity.setId(1L);
        schemaEntity.setType("user");
        schemaEntity.setVersion("1.0");
        schemaEntity.setSchemaContent("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"email\":{\"type\":\"string\"}},\"required\":[\"name\",\"email\"]}");

        ValidationRequest request = new ValidationRequest();
        request.setType("user");
        request.setVersion("1.0");
        request.setJsonData("{\"name\":\"John\",\"email\":\"john@example.com\"}");

        when(repository.findByTypeAndVersion("user", "1.0")).thenReturn(Optional.of(schemaEntity));

        // Act
        ValidationResponse response = jsonSchemaService.validateJson(request);

        // Assert
        assertThat(response.isValid()).isTrue();
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should return validation errors for invalid JSON")
    void shouldReturnValidationErrors() {
        // Arrange
        JsonSchemaEntity schemaEntity = new JsonSchemaEntity();
        schemaEntity.setId(1L);
        schemaEntity.setType("user");
        schemaEntity.setVersion("1.0");
        schemaEntity.setSchemaContent("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"email\":{\"type\":\"string\"}},\"required\":[\"name\",\"email\"]}");

        ValidationRequest request = new ValidationRequest();
        request.setType("user");
        request.setVersion("1.0");
        request.setJsonData("{\"name\":\"John\"}"); // Missing required email

        when(repository.findByTypeAndVersion("user", "1.0")).thenReturn(Optional.of(schemaEntity));

        // Act
        ValidationResponse response = jsonSchemaService.validateJson(request);

        // Assert
        assertThat(response.isValid()).isFalse();
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors().get(0)).contains("email");
    }

    @Test
    @DisplayName("Should throw exception when schema not found for validation")
    void shouldThrowExceptionWhenSchemaNotFoundForValidation() {
        // Arrange
        ValidationRequest request = new ValidationRequest();
        request.setType("nonexistent");
        request.setVersion("1.0");
        request.setJsonData("{\"name\":\"John\"}");

        when(repository.findByTypeAndVersion("nonexistent", "1.0")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jsonSchemaService.validateJson(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Schema not found");
    }

    @Test
    @DisplayName("Should get Avro schema successfully")
    void shouldGetAvroSchema() {
        // Arrange
        JsonSchemaEntity schemaEntity = new JsonSchemaEntity();
        schemaEntity.setId(1L);
        schemaEntity.setType("user");
        schemaEntity.setVersion("1.0");
        schemaEntity.setSchemaContent("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");

        String expectedAvroSchema = "{\"type\":\"record\",\"name\":\"User\"}";

        when(repository.findByTypeAndVersion("user", "1.0")).thenReturn(Optional.of(schemaEntity));
        when(avroConverterService.convertJsonSchemaToAvro(anyString(), eq("user")))
            .thenReturn(expectedAvroSchema);

        // Act
        String result = jsonSchemaService.getAvroSchema("user", "1.0");

        // Assert
        assertThat(result).isEqualTo(expectedAvroSchema);
        verify(avroConverterService).convertJsonSchemaToAvro(schemaEntity.getSchemaContent(), "user");
    }

    @Test
    @DisplayName("Should throw exception when schema not found for Avro conversion")
    void shouldThrowExceptionWhenSchemaNotFoundForAvro() {
        // Arrange
        when(repository.findByTypeAndVersion("nonexistent", "1.0")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> jsonSchemaService.getAvroSchema("nonexistent", "1.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Schema not found");
    }
}
