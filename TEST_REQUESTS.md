# Test Requests for JSON Schema Validation POC

## 1. Save a User Schema

**Using file (RECOMMENDED - no escaping issues):**
```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d @examples/save-user-schema-request.json
```

**Or inline (note the escaping):**
```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{"type":"user","version":"1.0","schemaContent":"{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\",\"minimum\":0},\"email\":{\"type\":\"string\",\"format\":\"email\"}},\"required\":[\"name\",\"email\"]}"}'
```

## 2. Validate Valid User JSON

**Using file (RECOMMENDED):**
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d @examples/validate-valid-request.json
```

**Or inline:**
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{"type":"user","version":"1.0","jsonData":"{\"name\":\"John Doe\",\"age\":30,\"email\":\"john@example.com\"}"}'
```

Expected Response:
```json
{
  "valid": true,
  "errors": []
}
```

## 3. Validate Invalid User JSON (missing required email)

**Using file (RECOMMENDED):**
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d @examples/validate-invalid-request.json
```

**Or inline:**
```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{"type":"user","version":"1.0","jsonData":"{\"name\":\"John Doe\",\"age\":30}"}'
```

Expected Response:
```json
{
  "valid": false,
  "errors": [
    "$: required property 'email' not found"
  ]
}
```

## 4. Validate Invalid User JSON (invalid age)

```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{"type":"user","version":"1.0","jsonData":"{\"name\":\"John Doe\",\"age\":-5,\"email\":\"john@example.com\"}"}'
```

Expected Response:
```json
{
  "valid": false,
  "errors": ["$.age: must have a minimum value of 0"]
}
```

## 5. Generate POJO JAR

```bash
curl -X GET "http://localhost:8080/api/generate-jar?type=user&version=1.0" \
  -o user-1.0-pojos.jar
```

## 6. Get Avro Schema

**Convert and retrieve the JSON Schema in Avro format:**

```bash
curl -X GET "http://localhost:8080/api/schemas/user/1.0/avro"
```

**Expected Response:**
```json
{
  "type": "user",
  "version": "1.0",
  "avroSchema": "{\n  \"type\" : \"record\",\n  \"name\" : \"User\",\n  \"namespace\" : \"org.example.generated\",\n  \"fields\" : [ {\n    \"name\" : \"name\",\n    \"type\" : \"string\"\n  }, {\n    \"name\" : \"age\",\n    \"type\" : [ \"null\", \"int\" ],\n    \"default\" : null\n  }, {\n    \"name\" : \"email\",\n    \"type\" : \"string\"\n  } ]\n}"
}
```

## Additional Test: Product Schema

### Save Product Schema

```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d '{
    "type": "product",
    "version": "1.0",
    "schemaContent": "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"},\"name\":{\"type\":\"string\"},\"price\":{\"type\":\"number\",\"minimum\":0},\"category\":{\"type\":\"string\",\"enum\":[\"electronics\",\"clothing\",\"food\"]}},\"required\":[\"id\",\"name\",\"price\"]}"
  }'
```

### Validate Product JSON

```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d '{
    "type": "product",
    "version": "1.0",
    "jsonData": "{\"id\":1,\"name\":\"Laptop\",\"price\":999.99,\"category\":\"electronics\"}"
  }'
```

### Generate Product POJO JAR

```bash
curl -X GET "http://localhost:8080/api/generate-jar?type=product&version=1.0" \
  -o product-1.0-pojos.jar
```

## Testing Nested Classes

### Save Person Schema with Nested Objects

This schema demonstrates **nested classes** including:
- `Address` object with nested `Coordinates` object
- Array of `PhoneNumber` objects
- `Employment` object with nested `Benefits` object

```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d @examples/save-nested-person-request.json
```

### Validate Nested Person JSON

```bash
curl -X POST http://localhost:8080/api/validate \
  -H "Content-Type: application/json" \
  -d @examples/validate-nested-person-request.json
```

### Generate POJO JAR with Nested Classes

```bash
curl -X GET "http://localhost:8080/api/generate-jar?type=person&version=2.0" \
  -o person-2.0-pojos.jar
```

### Get Avro Schema for Nested Person

```bash
curl -X GET "http://localhost:8080/api/schemas/person/2.0/avro"
```

**This will return the nested Avro schema with:**
- Nested record types for Address, Coordinates, Employment, Benefits
- Array of PhoneNumber records
- Proper field mappings and nullability


**This will generate multiple Java classes:**
- `Person.java` - Main class
- `Address.java` - Nested class for address
- `Coordinates.java` - Nested class for coordinates (inside Address)
- `PhoneNumber.java` - Class for phone number items in array
- `Employment.java` - Nested class for employment
- `Benefits.java` - Nested class for benefits (inside Employment)

All classes will include:
- Jackson annotations for JSON serialization/deserialization
- Getters and setters
- Builder pattern support
- hashCode() and equals() methods
- toString() method
