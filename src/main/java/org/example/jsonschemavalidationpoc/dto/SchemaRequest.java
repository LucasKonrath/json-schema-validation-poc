package org.example.jsonschemavalidationpoc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaRequest {
    private String type;
    private String version;
    private String schemaContent;
}
