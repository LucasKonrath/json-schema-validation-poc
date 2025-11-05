package org.example.jsonschemavalidationpoc.repository;

import org.example.jsonschemavalidationpoc.entity.JsonSchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JsonSchemaRepository extends JpaRepository<JsonSchemaEntity, Long> {
    
    Optional<JsonSchemaEntity> findByTypeAndVersion(String type, String version);
    
    boolean existsByTypeAndVersion(String type, String version);
}
