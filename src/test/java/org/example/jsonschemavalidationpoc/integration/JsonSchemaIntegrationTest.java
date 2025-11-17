package org.example.jsonschemavalidationpoc.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.example.jsonschemavalidationpoc.entity.JsonSchemaEntity;
import org.example.jsonschemavalidationpoc.repository.JsonSchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("JSON Schema API Integration Tests")
class JsonSchemaIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JsonSchemaRepository repository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should save a new JSON schema successfully")
    void shouldSaveSchema() {
        String schemaRequest = """
            {
                "type": "user",
                "version": "1.0",
                "schemaContent": "{\\"$schema\\":\\"http://json-schema.org/draft-07/schema#\\",\\"type\\":\\"object\\",\\"properties\\":{\\"name\\":{\\"type\\":\\"string\\"},\\"email\\":{\\"type\\":\\"string\\"}},\\"required\\":[\\"name\\",\\"email\\"]}"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(schemaRequest)
        .when()
            .post("/api/schemas")
        .then()
            .statusCode(201)
            .body("type", equalTo("user"))
            .body("version", equalTo("1.0"))
            .body("schemaContent", containsString("$schema"))
            .body("id", notNullValue());
    }

    @Test
    @DisplayName("Should reject invalid JSON schema content")
    void shouldRejectInvalidSchema() {
        String invalidSchemaRequest = """
            {
                "type": "user",
                "version": "1.0",
                "schemaContent": "not a valid json"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(invalidSchemaRequest)
        .when()
            .post("/api/schemas")
        .then()
            .statusCode(400)
            .body(containsString("Invalid JSON schema"));
    }

    @Test
    @DisplayName("Should reject duplicate schema type and version")
    void shouldRejectDuplicateSchema() {
        // Save first schema
        JsonSchemaEntity entity = new JsonSchemaEntity();
        entity.setType("user");
        entity.setVersion("1.0");
        entity.setSchemaContent("{\"type\":\"object\"}");
        repository.save(entity);

        // Try to save duplicate
        String duplicateRequest = """
            {
                "type": "user",
                "version": "1.0",
                "schemaContent": "{\\"type\\":\\"object\\"}"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(duplicateRequest)
        .when()
            .post("/api/schemas")
        .then()
            .statusCode(400)
            .body(containsString("already exists"));
    }

    @Test
    @DisplayName("Should validate JSON successfully against schema")
    void shouldValidateJsonSuccessfully() {
        // First, save a schema
        JsonSchemaEntity entity = new JsonSchemaEntity();
        entity.setType("user");
        entity.setVersion("1.0");
        entity.setSchemaContent("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\",\"minimum\":0},\"email\":{\"type\":\"string\"}},\"required\":[\"name\",\"email\"]}");
        repository.save(entity);

        // Validate valid JSON
        String validationRequest = """
            {
                "type": "user",
                "version": "1.0",
                "jsonData": "{\\"name\\":\\"John Doe\\",\\"age\\":30,\\"email\\":\\"john@example.com\\"}"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(validationRequest)
        .when()
            .post("/api/validate")
        .then()
            .statusCode(200)
            .body("valid", equalTo(true))
            .body("errors", empty());
    }

    @Test
    @DisplayName("Should return validation errors for invalid JSON")
    void shouldReturnValidationErrors() {
        // First, save a schema
        JsonSchemaEntity entity = new JsonSchemaEntity();
        entity.setType("user");
        entity.setVersion("1.0");
        entity.setSchemaContent("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"email\":{\"type\":\"string\"}},\"required\":[\"name\",\"email\"]}");
        repository.save(entity);

        // Validate invalid JSON (missing required field)
        String validationRequest = """
            {
                "type": "user",
                "version": "1.0",
                "jsonData": "{\\"name\\":\\"John Doe\\"}"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(validationRequest)
        .when()
            .post("/api/validate")
        .then()
            .statusCode(200)
            .body("valid", equalTo(false))
            .body("errors", not(empty()))
            .body("errors[0]", containsString("email"));
    }

    @Test
    @DisplayName("Should return 400 when validating against non-existent schema")
    void shouldReturn400ForNonExistentSchema() {
        String validationRequest = """
            {
                "type": "nonexistent",
                "version": "1.0",
                "jsonData": "{\\"name\\":\\"John\\"}"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(validationRequest)
        .when()
            .post("/api/validate")
        .then()
            .statusCode(400)
            .body("errors[0]", containsString("Schema not found"));
    }

    @Test
    @DisplayName("Should generate POJO JAR file successfully")
    void shouldGeneratePojoJar() {
        // First, save a schema
        JsonSchemaEntity entity = new JsonSchemaEntity();
        entity.setType("user");
        entity.setVersion("1.0");
        entity.setSchemaContent("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"email\":{\"type\":\"string\"}},\"required\":[\"name\",\"email\"]}");
        repository.save(entity);

        given()
            .queryParam("type", "user")
            .queryParam("version", "1.0")
        .when()
            .get("/api/generate-jar")
        .then()
            .statusCode(200)
            .contentType("application/octet-stream")
            .header("Content-Disposition", containsString("user-1.0-pojos.jar"));
    }

    @Test
    @DisplayName("Should return 400 when generating JAR for non-existent schema")
    void shouldReturn400ForNonExistentSchemaJar() {
        given()
            .queryParam("type", "nonexistent")
            .queryParam("version", "1.0")
        .when()
            .get("/api/generate-jar")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should convert JSON schema to Avro successfully")
    void shouldConvertToAvro() {
        // First, save a schema
        JsonSchemaEntity entity = new JsonSchemaEntity();
        entity.setType("user");
        entity.setVersion("1.0");
        entity.setSchemaContent("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"},\"email\":{\"type\":\"string\"}},\"required\":[\"name\",\"email\"]}");
        repository.save(entity);

        given()
            .pathParam("type", "user")
            .pathParam("version", "1.0")
        .when()
            .get("/api/schemas/{type}/{version}/avro")
        .then()
            .statusCode(200)
            .body("type", equalTo("user"))
            .body("version", equalTo("1.0"))
            .body("avroSchema", containsString("\"type\" : \"record\""))
            .body("avroSchema", containsString("\"name\" : \"User\""))
            .body("avroSchema", containsString("org.example.generated"));
    }

    @Test
    @DisplayName("Should return 400 when converting non-existent schema to Avro")
    void shouldReturn400ForNonExistentSchemaAvro() {
        given()
            .pathParam("type", "nonexistent")
            .pathParam("version", "1.0")
        .when()
            .get("/api/schemas/{type}/{version}/avro")
        .then()
            .statusCode(400)
            .body(containsString("Schema not found"));
    }

    @Test
    @DisplayName("Should handle nested objects in Avro conversion")
    void shouldConvertNestedObjectsToAvro() {
        // Save schema with nested objects
        JsonSchemaEntity entity = new JsonSchemaEntity();
        entity.setType("person");
        entity.setVersion("2.0");
        entity.setSchemaContent("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"address\":{\"type\":\"object\",\"properties\":{\"street\":{\"type\":\"string\"},\"city\":{\"type\":\"string\"}},\"required\":[\"street\",\"city\"]}},\"required\":[\"name\",\"address\"]}");
        repository.save(entity);

        given()
            .pathParam("type", "person")
            .pathParam("version", "2.0")
        .when()
            .get("/api/schemas/{type}/{version}/avro")
        .then()
            .statusCode(200)
            .body("avroSchema", containsString("\"type\" : \"record\""))
            .body("avroSchema", containsString("\"name\" : \"address\""));
    }
}
