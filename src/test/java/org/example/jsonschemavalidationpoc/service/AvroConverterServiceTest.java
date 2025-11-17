package org.example.jsonschemavalidationpoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AvroConverterService Unit Tests")
class AvroConverterServiceTest {

    private AvroConverterService avroConverterService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        avroConverterService = new AvroConverterService(objectMapper);
    }

    @Test
    @DisplayName("Should convert simple JSON schema to Avro")
    void shouldConvertSimpleSchema() {
        // Arrange
        String jsonSchema = """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "age": {"type": "integer"}
                },
                "required": ["name"]
            }
            """;

        // Act
        String avroSchema = avroConverterService.convertJsonSchemaToAvro(jsonSchema, "User");

        // Assert
        assertThat(avroSchema).isNotNull();
        assertThat(avroSchema).contains("\"type\" : \"record\"");
        assertThat(avroSchema).contains("\"name\" : \"User\"");
        assertThat(avroSchema).contains("org.example.generated");
        assertThat(avroSchema).contains("\"name\" : \"name\"");
        assertThat(avroSchema).contains("\"name\" : \"age\"");
    }

    @Test
    @DisplayName("Should handle required and optional fields correctly")
    void shouldHandleRequiredAndOptionalFields() {
        // Arrange
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "requiredField": {"type": "string"},
                    "optionalField": {"type": "string"}
                },
                "required": ["requiredField"]
            }
            """;

        // Act
        String avroSchema = avroConverterService.convertJsonSchemaToAvro(jsonSchema, "TestRecord");

        // Assert
        assertThat(avroSchema).contains("\"name\" : \"requiredField\"");
        assertThat(avroSchema).contains("\"name\" : \"optionalField\"");
        // Optional fields should have union with null
        assertThat(avroSchema).contains("\"null\"");
    }

    @Test
    @DisplayName("Should convert nested objects to nested Avro records")
    void shouldConvertNestedObjects() {
        // Arrange
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "address": {
                        "type": "object",
                        "properties": {
                            "street": {"type": "string"},
                            "city": {"type": "string"}
                        },
                        "required": ["street", "city"]
                    }
                },
                "required": ["name", "address"]
            }
            """;

        // Act
        String avroSchema = avroConverterService.convertJsonSchemaToAvro(jsonSchema, "Person");

        // Assert
        assertThat(avroSchema).contains("\"name\" : \"Person\"");
        assertThat(avroSchema).contains("\"name\" : \"address\"");
        assertThat(avroSchema).contains("\"name\" : \"street\"");
        assertThat(avroSchema).contains("\"name\" : \"city\"");
    }

    @Test
    @DisplayName("Should convert arrays to Avro arrays")
    void shouldConvertArrays() {
        // Arrange
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "tags": {
                        "type": "array",
                        "items": {"type": "string"}
                    }
                },
                "required": ["tags"]
            }
            """;

        // Act
        String avroSchema = avroConverterService.convertJsonSchemaToAvro(jsonSchema, "Document");

        // Assert
        assertThat(avroSchema).contains("\"name\" : \"tags\"");
        assertThat(avroSchema).contains("\"type\" : \"array\"");
    }

    @Test
    @DisplayName("Should convert enums to Avro enums")
    void shouldConvertEnums() {
        // Arrange
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "status": {
                        "type": "string",
                        "enum": ["active", "inactive", "pending"]
                    }
                },
                "required": ["status"]
            }
            """;

        // Act
        String avroSchema = avroConverterService.convertJsonSchemaToAvro(jsonSchema, "Status");

        // Assert
        assertThat(avroSchema).contains("\"name\" : \"status\"");
        assertThat(avroSchema).contains("\"type\" : \"enum\"");
    }

    @Test
    @DisplayName("Should map JSON Schema types to Avro types correctly")
    void shouldMapTypesCorrectly() {
        // Arrange
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "stringField": {"type": "string"},
                    "intField": {"type": "integer"},
                    "numberField": {"type": "number"},
                    "boolField": {"type": "boolean"}
                },
                "required": ["stringField", "intField", "numberField", "boolField"]
            }
            """;

        // Act
        String avroSchema = avroConverterService.convertJsonSchemaToAvro(jsonSchema, "TypeTest");

        // Assert
        assertThat(avroSchema).contains("\"name\" : \"stringField\"");
        assertThat(avroSchema).contains("\"name\" : \"intField\"");
        assertThat(avroSchema).contains("\"name\" : \"numberField\"");
        assertThat(avroSchema).contains("\"name\" : \"boolField\"");
        assertThat(avroSchema).contains("\"string\"");
        assertThat(avroSchema).contains("\"int\"");
        assertThat(avroSchema).contains("\"double\"");
        assertThat(avroSchema).contains("\"boolean\"");
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON schema")
    void shouldThrowExceptionForInvalidSchema() {
        // Arrange
        String invalidSchema = "not a valid json";

        // Act & Assert
        assertThatThrownBy(() -> avroConverterService.convertJsonSchemaToAvro(invalidSchema, "Test"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error converting JSON Schema to Avro");
    }

    @Test
    @DisplayName("Should handle complex nested structure")
    void shouldHandleComplexNestedStructure() {
        // Arrange
        String jsonSchema = """
            {
                "type": "object",
                "properties": {
                    "id": {"type": "integer"},
                    "profile": {
                        "type": "object",
                        "properties": {
                            "name": {"type": "string"},
                            "contacts": {
                                "type": "array",
                                "items": {
                                    "type": "object",
                                    "properties": {
                                        "type": {"type": "string"},
                                        "value": {"type": "string"}
                                    }
                                }
                            }
                        },
                        "required": ["name"]
                    }
                },
                "required": ["id", "profile"]
            }
            """;

        // Act
        String avroSchema = avroConverterService.convertJsonSchemaToAvro(jsonSchema, "ComplexUser");

        // Assert
        assertThat(avroSchema).contains("\"name\" : \"ComplexUser\"");
        assertThat(avroSchema).contains("\"name\" : \"id\"");
        assertThat(avroSchema).contains("\"name\" : \"profile\"");
        assertThat(avroSchema).contains("\"name\" : \"contacts\"");
        assertThat(avroSchema).contains("\"type\" : \"array\"");
    }
}
