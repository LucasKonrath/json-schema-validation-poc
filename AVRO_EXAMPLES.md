# Avro Conversion Examples

## Simple User Schema to Avro

### 1. First, save the user schema:
```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d @examples/save-user-schema-request.json
```

### 2. Get the Avro version:
```bash
curl -X GET "http://localhost:8080/api/schemas/user/1.0/avro"
```

### Expected Avro Schema Output:
```json
{
  "type": "user",
  "version": "1.0",
  "avroSchema": "{\n  \"type\" : \"record\",\n  \"name\" : \"User\",\n  \"namespace\" : \"org.example.generated\",\n  \"fields\" : [ {\n    \"name\" : \"name\",\n    \"type\" : \"string\"\n  }, {\n    \"name\" : \"age\",\n    \"type\" : [ \"null\", \"int\" ],\n    \"default\" : null\n  }, {\n    \"name\" : \"email\",\n    \"type\" : \"string\"\n  } ]\n}"
}
```

## Nested Person Schema to Avro

### 1. Save the nested person schema:
```bash
curl -X POST http://localhost:8080/api/schemas \
  -H "Content-Type: application/json" \
  -d @examples/save-nested-person-request.json
```

### 2. Get the Avro version:
```bash
curl -X GET "http://localhost:8080/api/schemas/person/2.0/avro"
```

### Expected Features in Avro Schema:
- **Main Person record** with fields: id, name, email, address, phoneNumbers, employment
- **Nested Address record** with Coordinates sub-record
- **Array of PhoneNumber records**
- **Nested Employment record** with Benefits sub-record
- **Proper nullability** - required fields are non-nullable, optional fields use union with null

## Key Mapping Rules

### JSON Schema Type → Avro Type
- `string` → `string`
- `integer` → `int`
- `number` → `double`
- `boolean` → `boolean`
- `object` → `record` (nested)
- `array` → `array`
- `enum` → `enum`

### Special Handling
- **Required fields**: Direct type mapping (e.g., `"type": "string"`)
- **Optional fields**: Union with null (e.g., `"type": ["null", "string"]`)
- **Nested objects**: Separate Avro record types
- **Date/DateTime**: Mapped to `long` (timestamp in milliseconds)
- **UUID**: Mapped to `string`

## Using the Avro Schema

The returned Avro schema can be used with:
- **Apache Kafka** - Register in Schema Registry
- **Apache Avro tools** - Generate code, validate data
- **Data serialization** - Efficient binary serialization
- **Schema evolution** - Track schema versions and compatibility
