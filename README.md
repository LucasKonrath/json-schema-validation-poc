# JSON Schema Validation POC

This is a proof of concept application that provides REST endpoints for managing JSON schemas, validating JSON data against schemas, and generating Java POJOs from schemas.

## Features

1. **Save JSON Schema**: Store JSON schemas in H2 database with type and version
2. **Validate JSON**: Validate JSON data against stored schemas
3. **Generate POJOs**: Generate and download JAR files containing Java POJOs based on schemas
4. **Avro Conversion**: Convert and retrieve JSON schemas in Apache Avro format

## Prerequisites

- Java 21
- Maven

## Running the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

## H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:jsonschemadb`
- Username: `sa`
- Password: (leave empty)

## API Endpoints

### 1. Save JSON Schema

**POST** `/api/schemas`

Save a new JSON schema to the database.

**Request Body:**
```json
{
  "type": "user",
  "version": "1.0",
  "schemaContent": "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\",\"minimum\":0},\"email\":{\"type\":\"string\",\"format\":\"email\"}},\"required\":[\"name\",\"email\"]}"
}
```

**Example using curl:**
```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{
    "type": "user",
    "version": "1.0",
    "schemaContent": "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\",\"minimum\":0},\"email\":{\"type\":\"string\",\"format\":\"email\"}},\"required\":[\"name\",\"email\"]}"
  }'
```

**Schema Content Example (formatted for readability):**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "age": {
      "type": "integer",
      "minimum": 0
    },
    "email": {
      "type": "string",
      "format": "email"
    }
  },
  "required": ["name", "email"]
}
```

### 2. Validate JSON

**POST** `/api/validate`

Validate JSON data against a stored schema.

**Request Body:**
```json
{
  "type": "user",
  "version": "1.0",
  "jsonData": "{\"name\":\"John Doe\",\"age\":30,\"email\":\"john@example.com\"}"
}
```

**Example using curl (valid JSON):**
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{
    "type": "user",
    "version": "1.0",
    "jsonData": "{\"name\":\"John Doe\",\"age\":30,\"email\":\"john@example.com\"}"
  }'
```

**Response (valid):**
```json
{
  "valid": true,
  "errors": []
}
```

**Example using curl (invalid JSON - missing required field):**
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{
    "type": "user",
    "version": "1.0",
    "jsonData": "{\"name\":\"John Doe\",\"age\":30}"
  }'
```

**Response (invalid):**
```json
{
  "valid": false,
  "errors": [
    "$.email: is missing but it is required"
  ]
}
```

### 3. Generate and Download POJO JAR

**GET** `/api/generate-jar?type={type}&version={version}`

Generate Java POJOs from a stored schema and download as a JAR file.

**Example using curl:**
```bash
curl -X GET "http://localhost:8080/api/generate-jar?type=user&version=1.0" \
  -o user-1.0-pojos.jar
```

**Example using browser:**
Open in your browser:
```
http://localhost:8080/api/generate-jar?type=user&version=1.0
```

This will download a JAR file containing the generated POJO classes based on the schema.

### 4. Get Avro Schema

**GET** `/api/schemas/{type}/{version}/avro`

Convert and retrieve a stored JSON schema in Apache Avro format.

**Example using curl:**
```bash
curl -X GET "http://localhost:8080/api/schemas/user/1.0/avro"
```

**Response:**
```json
{
  "type": "user",
  "version": "1.0",
  "avroSchema": "{\n  \"type\" : \"record\",\n  \"name\" : \"User\",\n  \"namespace\" : \"org.example.generated\",\n  \"fields\" : [ {\n    \"name\" : \"name\",\n    \"type\" : \"string\"\n  }, {\n    \"name\" : \"age\",\n    \"type\" : [ \"null\", \"int\" ],\n    \"default\" : null\n  }, {\n    \"name\" : \"email\",\n    \"type\" : \"string\"\n  } ]\n}"
}
```

**Features:**
- Converts JSON Schema types to Avro types
- Handles nested objects as Avro records
- Supports arrays and enums
- Maps required/optional fields to Avro unions with null
- Preserves schema structure and relationships

## Example Workflow

1. **Start the application**
   ```bash
   ./mvnw spring-boot:run
   ```

2. **Save a schema**
   ```bash
   curl -X POST http://localhost:8080/api/schemas \
     -H "Content-Type: application/json" \
     -d '{
       "type": "product",
       "version": "1.0",
       "schemaContent": "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"},\"name\":{\"type\":\"string\"},\"price\":{\"type\":\"number\",\"minimum\":0}},\"required\":[\"id\",\"name\",\"price\"]}"
     }'
   ```

3. **Validate valid JSON**
   ```bash
   curl -X POST http://localhost:8080/api/validate \
     -H "Content-Type: application/json" \
     -d '{
       "type": "product",
       "version": "1.0",
       "jsonData": "{\"id\":1,\"name\":\"Laptop\",\"price\":999.99}"
     }'
   ```

4. **Validate invalid JSON**
   ```bash
   curl -X POST http://localhost:8080/api/validate \
     -H "Content-Type: application/json" \
     -d '{
       "type": "product",
       "version": "1.0",
       "jsonData": "{\"id\":1,\"name\":\"Laptop\"}"
     }'
   ```

5. **Generate POJO JAR**
   ```bash
   curl -X GET "http://localhost:8080/api/generate-jar?type=product&version=1.0" \
     -o product-1.0-pojos.jar
   ```

6. **Get Avro Schema**
   ```bash
   curl -X GET "http://localhost:8080/api/schemas/product/1.0/avro"
   ```

## Technologies Used

- **Spring Boot 3.5.7** - Main framework
- **Spring Data JPA** - Database operations
- **H2 Database** - In-memory database
- **NetworkNT JSON Schema Validator** - JSON schema validation
- **jsonschema2pojo** - POJO generation from JSON schemas
- **Apache Avro** - Avro schema conversion
- **Lombok** - Reduce boilerplate code

## Notes

- The H2 database is in-memory, so all data is lost when the application stops
- Schema type and version combinations must be unique
- The POJO generation creates Java source files packaged in a JAR
- Generated POJOs include Jackson annotations for JSON serialization/deserialization
- Avro conversion supports nested objects, arrays, enums, and proper type mappings
- Required fields in JSON Schema are mapped as non-nullable in Avro
- Optional fields use Avro unions with null type
