package org.example.jsonschemavalidationpoc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "json_schemas", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"type", "version"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JsonSchemaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String type;
    
    @Column(nullable = false)
    private String version;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String schemaContent;
}
