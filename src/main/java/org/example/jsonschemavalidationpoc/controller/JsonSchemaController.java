package org.example.jsonschemavalidationpoc.controller;

import lombok.RequiredArgsConstructor;
import org.example.jsonschemavalidationpoc.dto.SchemaRequest;
import org.example.jsonschemavalidationpoc.dto.ValidationRequest;
import org.example.jsonschemavalidationpoc.dto.ValidationResponse;
import org.example.jsonschemavalidationpoc.entity.JsonSchemaEntity;
import org.example.jsonschemavalidationpoc.service.JsonSchemaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JsonSchemaController {
    
    private final JsonSchemaService jsonSchemaService;
    
    @PostMapping("/schemas")
    public ResponseEntity<?> saveSchema(@RequestBody SchemaRequest request) {
        try {
            JsonSchemaEntity saved = jsonSchemaService.saveSchema(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error saving schema: " + e.getMessage());
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateJson(@RequestBody ValidationRequest request) {
        try {
            ValidationResponse response = jsonSchemaService.validateJson(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                new ValidationResponse(false, java.util.Collections.singletonList(e.getMessage())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ValidationResponse(false, 
                    java.util.Collections.singletonList("Validation error: " + e.getMessage())));
        }
    }
    
    @GetMapping("/generate-jar")
    public ResponseEntity<byte[]> generateJar(
            @RequestParam String type, 
            @RequestParam String version) {
        try {
            byte[] jarBytes = jsonSchemaService.generatePojoJar(type, version);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                type + "-" + version + "-pojos.jar");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(jarBytes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
