package org.example.jsonschemavalidationpoc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AvroConverterService {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Converts a JSON Schema to Avro Schema format
     */
    public String convertJsonSchemaToAvro(String jsonSchemaContent, String schemaName) {
        try {
            JsonNode schemaNode = objectMapper.readTree(jsonSchemaContent);
            Schema avroSchema = convertToAvroSchema(schemaNode, schemaName);
            return avroSchema.toString(true); // pretty print
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON Schema to Avro: " + e.getMessage(), e);
        }
    }
    
    private Schema convertToAvroSchema(JsonNode jsonSchema, String name) {
        String type = jsonSchema.has("type") ? jsonSchema.get("type").asText() : "object";
        
        switch (type) {
            case "object":
                return convertObjectToAvro(jsonSchema, name);
            case "array":
                return convertArrayToAvro(jsonSchema, name);
            case "string":
                return handleStringType(jsonSchema);
            case "integer":
                return Schema.create(Schema.Type.INT);
            case "number":
                return Schema.create(Schema.Type.DOUBLE);
            case "boolean":
                return Schema.create(Schema.Type.BOOLEAN);
            case "null":
                return Schema.create(Schema.Type.NULL);
            default:
                return Schema.create(Schema.Type.STRING);
        }
    }
    
    private Schema handleStringType(JsonNode jsonSchema) {
        if (jsonSchema.has("format")) {
            String format = jsonSchema.get("format").asText();
            // Handle special string formats
            switch (format) {
                case "date":
                case "date-time":
                    return Schema.create(Schema.Type.LONG); // timestamp in millis
                case "uuid":
                    return Schema.create(Schema.Type.STRING);
                default:
                    return Schema.create(Schema.Type.STRING);
            }
        }
        
        // Handle enums
        if (jsonSchema.has("enum")) {
            List<String> symbols = new ArrayList<>();
            jsonSchema.get("enum").forEach(node -> symbols.add(node.asText()));
            return Schema.createEnum("EnumType", null, null, symbols);
        }
        
        return Schema.create(Schema.Type.STRING);
    }
    
    private Schema convertObjectToAvro(JsonNode jsonSchema, String name) {
        SchemaBuilder.RecordBuilder<Schema> recordBuilder = SchemaBuilder.record(capitalizeFirstLetter(name));
        recordBuilder.namespace("org.example.generated");
        
        SchemaBuilder.FieldAssembler<Schema> fieldAssembler = recordBuilder.fields();
        
        JsonNode properties = jsonSchema.get("properties");
        Set<String> requiredFields = getRequiredFields(jsonSchema);
        
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldSchema = field.getValue();
                
                boolean isRequired = requiredFields.contains(fieldName);
                Schema fieldType = convertToAvroSchema(fieldSchema, fieldName);
                
                if (isRequired) {
                    fieldAssembler.name(fieldName).type(fieldType).noDefault();
                } else {
                    // Optional field - use union with null
                    fieldAssembler.name(fieldName)
                        .type()
                        .unionOf()
                        .nullType()
                        .and()
                        .type(fieldType)
                        .endUnion()
                        .nullDefault();
                }
            }
        }
        
        return fieldAssembler.endRecord();
    }
    
    private Schema convertArrayToAvro(JsonNode jsonSchema, String name) {
        if (jsonSchema.has("items")) {
            JsonNode items = jsonSchema.get("items");
            Schema itemsSchema = convertToAvroSchema(items, name + "Item");
            return Schema.createArray(itemsSchema);
        }
        // Default to array of strings if items not specified
        return Schema.createArray(Schema.create(Schema.Type.STRING));
    }
    
    private Set<String> getRequiredFields(JsonNode jsonSchema) {
        Set<String> required = new HashSet<>();
        if (jsonSchema.has("required") && jsonSchema.get("required").isArray()) {
            jsonSchema.get("required").forEach(node -> required.add(node.asText()));
        }
        return required;
    }
    
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // Remove special characters and capitalize
        String cleaned = str.replaceAll("[^a-zA-Z0-9]", "");
        return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }
}
