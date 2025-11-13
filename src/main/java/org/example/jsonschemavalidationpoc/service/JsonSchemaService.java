package org.example.jsonschemavalidationpoc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.sun.codemodel.JCodeModel;
import org.example.jsonschemavalidationpoc.dto.SchemaRequest;
import org.example.jsonschemavalidationpoc.dto.ValidationRequest;
import org.example.jsonschemavalidationpoc.dto.ValidationResponse;
import org.example.jsonschemavalidationpoc.entity.JsonSchemaEntity;
import org.example.jsonschemavalidationpoc.repository.JsonSchemaRepository;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JsonSchemaService {
    
    private final JsonSchemaRepository repository;
    private final ObjectMapper objectMapper;
    private final AvroConverterService avroConverterService;
    
    public JsonSchemaEntity saveSchema(SchemaRequest request) {
        // Validate that the schema is valid JSON
        try {
            objectMapper.readTree(request.getSchemaContent());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON schema: " + e.getMessage());
        }
        
        // Check if schema with same type and version already exists
        if (repository.existsByTypeAndVersion(request.getType(), request.getVersion())) {
            throw new IllegalArgumentException("Schema with type '" + request.getType() + 
                "' and version '" + request.getVersion() + "' already exists");
        }
        
        JsonSchemaEntity entity = new JsonSchemaEntity();
        entity.setType(request.getType());
        entity.setVersion(request.getVersion());
        entity.setSchemaContent(request.getSchemaContent());
        
        return repository.save(entity);
    }
    
    public ValidationResponse validateJson(ValidationRequest request) {
        // Find the schema
        JsonSchemaEntity schemaEntity = repository.findByTypeAndVersion(
            request.getType(), request.getVersion())
            .orElseThrow(() -> new IllegalArgumentException(
                "Schema not found for type '" + request.getType() + 
                "' and version '" + request.getVersion() + "'"));
        
        try {
            // Parse the schema and JSON data
            JsonNode schemaNode = objectMapper.readTree(schemaEntity.getSchemaContent());
            JsonNode jsonNode = objectMapper.readTree(request.getJsonData());
            
            // Create validator
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = factory.getSchema(schemaNode);
            
            // Validate
            Set<ValidationMessage> validationMessages = schema.validate(jsonNode);
            
            if (validationMessages.isEmpty()) {
                return new ValidationResponse(true, Collections.emptyList());
            } else {
                List<String> errors = validationMessages.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());
                return new ValidationResponse(false, errors);
            }
        } catch (Exception e) {
            return new ValidationResponse(false, 
                Collections.singletonList("Validation error: " + e.getMessage()));
        }
    }
    
    public byte[] generatePojoJar(String type, String version) {
        // Find the schema
        JsonSchemaEntity schemaEntity = repository.findByTypeAndVersion(type, version)
            .orElseThrow(() -> new IllegalArgumentException(
                "Schema not found for type '" + type + "' and version '" + version + "'"));
        
        try {
            // Create temporary directories
            Path tempDir = Files.createTempDirectory("pojo-gen-");
            Path sourceDir = tempDir.resolve("src");
            Path outputDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            Files.createDirectories(outputDir);
            
            // Write schema to temporary file
            Path schemaFile = tempDir.resolve("schema.json");
            Files.write(schemaFile, schemaEntity.getSchemaContent().getBytes());
            
            // Configure jsonschema2pojo
            JCodeModel codeModel = new JCodeModel();
            
            GenerationConfig config = new DefaultGenerationConfig() {
                @Override
                public boolean isGenerateBuilders() {
                    return true;
                }
                
                @Override
                public SourceType getSourceType() {
                    return SourceType.JSONSCHEMA;
                }
                
                @Override
                public boolean isIncludeHashcodeAndEquals() {
                    return true;
                }
                
                @Override
                public boolean isIncludeToString() {
                    return true;
                }
                
                @Override
                public AnnotationStyle getAnnotationStyle() {
                    return AnnotationStyle.JACKSON2;
                }
            };
            
            SchemaMapper mapper = new SchemaMapper(
                new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), 
                new SchemaGenerator()
            );
            
            // Generate POJOs
            mapper.generate(codeModel, 
                capitalizeFirstLetter(type), 
                "org.example.generated", 
                schemaFile.toUri().toURL());
            
            codeModel.build(sourceDir.toFile());
            
            // Create JAR file
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JarOutputStream jos = new JarOutputStream(baos)) {
                addFilesToJar(jos, sourceDir, sourceDir);
            }
            
            // Cleanup
            deleteDirectory(tempDir);
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error generating POJO JAR: " + e.getMessage(), e);
        }
    }
    
    private void addFilesToJar(JarOutputStream jos, Path baseDir, Path sourceDir) throws IOException {
        Files.walk(sourceDir)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    Path relativePath = baseDir.relativize(file);
                    JarEntry entry = new JarEntry(relativePath.toString().replace("\\", "/"));
                    jos.putNextEntry(entry);
                    Files.copy(file, jos);
                    jos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore
                    }
                });
        }
    }
    
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    public String getAvroSchema(String type, String version) {
        // Find the schema
        JsonSchemaEntity schemaEntity = repository.findByTypeAndVersion(type, version)
            .orElseThrow(() -> new IllegalArgumentException(
                "Schema not found for type '" + type + "' and version '" + version + "'"));
        
        // Convert to Avro
        return avroConverterService.convertJsonSchemaToAvro(
            schemaEntity.getSchemaContent(), 
            type
        );
    }
}
