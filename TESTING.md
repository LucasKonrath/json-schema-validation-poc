# Integration Tests Documentation

## Overview

This document describes the integration and unit tests for the JSON Schema Validation POC.

## Test Coverage

### Integration Tests (`JsonSchemaIntegrationTest`)

Located in: `src/test/java/org/example/jsonschemavalidationpoc/integration/JsonSchemaIntegrationTest.java`

#### POST /api/schemas
- ✅ **shouldSaveSchema** - Validates successful schema creation
- ✅ **shouldRejectInvalidSchema** - Validates rejection of invalid JSON content
- ✅ **shouldRejectDuplicateSchema** - Validates uniqueness constraint on type+version

#### POST /api/validate
- ✅ **shouldValidateJsonSuccessfully** - Validates successful JSON validation
- ✅ **shouldReturnValidationErrors** - Validates error reporting for invalid JSON
- ✅ **shouldReturn400ForNonExistentSchema** - Validates error handling for missing schema

#### GET /api/generate-jar
- ✅ **shouldGeneratePojoJar** - Validates JAR generation and download
- ✅ **shouldReturn400ForNonExistentSchemaJar** - Validates error handling for missing schema

#### GET /api/schemas/{type}/{version}/avro
- ✅ **shouldConvertToAvro** - Validates Avro conversion
- ✅ **shouldReturn400ForNonExistentSchemaAvro** - Validates error handling for missing schema
- ✅ **shouldConvertNestedObjectsToAvro** - Validates nested object conversion

### Unit Tests

#### JsonSchemaService Tests (`JsonSchemaServiceTest`)

Located in: `src/test/java/org/example/jsonschemavalidationpoc/service/JsonSchemaServiceTest.java`

- ✅ **shouldSaveValidSchema** - Tests schema save logic
- ✅ **shouldThrowExceptionForInvalidSchema** - Tests validation error handling
- ✅ **shouldThrowExceptionForDuplicateSchema** - Tests duplicate prevention
- ✅ **shouldValidateJsonSuccessfully** - Tests JSON validation logic
- ✅ **shouldReturnValidationErrors** - Tests validation error collection
- ✅ **shouldThrowExceptionWhenSchemaNotFoundForValidation** - Tests missing schema handling
- ✅ **shouldGetAvroSchema** - Tests Avro retrieval
- ✅ **shouldThrowExceptionWhenSchemaNotFoundForAvro** - Tests missing schema for Avro

#### AvroConverterService Tests (`AvroConverterServiceTest`)

Located in: `src/test/java/org/example/jsonschemavalidationpoc/service/AvroConverterServiceTest.java`

- ✅ **shouldConvertSimpleSchema** - Tests basic schema conversion
- ✅ **shouldHandleRequiredAndOptionalFields** - Tests field nullability
- ✅ **shouldConvertNestedObjects** - Tests nested object conversion
- ✅ **shouldConvertArrays** - Tests array conversion
- ✅ **shouldConvertEnums** - Tests enum conversion
- ✅ **shouldMapTypesCorrectly** - Tests type mapping (string, int, double, boolean)
- ✅ **shouldThrowExceptionForInvalidSchema** - Tests error handling
- ✅ **shouldHandleComplexNestedStructure** - Tests complex nested structures

## Running the Tests

### Run all tests
```bash
./mvnw test
```

### Run integration tests only
```bash
./mvnw test -Dtest=JsonSchemaIntegrationTest
```

### Run unit tests only
```bash
./mvnw test -Dtest=JsonSchemaServiceTest,AvroConverterServiceTest
```

### Run tests with coverage
```bash
./mvnw test jacoco:report
```

## Test Configuration

### Test Profile
Tests use the `test` profile with configuration in `src/test/resources/application-test.properties`:
- Uses in-memory H2 database: `jdbc:h2:mem:testdb`
- Auto-creates schema on startup
- SQL logging disabled for cleaner test output
- H2 console disabled in tests

### Test Dependencies
- **Spring Boot Starter Test** - Core testing framework
- **REST Assured** - API testing framework
- **Hamcrest** - Assertion matchers
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

## Test Patterns

### Integration Tests
- Use `@SpringBootTest` with random port
- Use `@ActiveProfiles("test")` for test configuration
- Clean database before each test with `@BeforeEach`
- Use REST Assured for API calls
- Verify HTTP status codes and response bodies

### Unit Tests
- Use `@ExtendWith(MockitoExtension.class)` for Mockito support
- Mock dependencies with `@Mock`
- Inject mocks with `@InjectMocks`
- Use AssertJ for fluent assertions
- Test both success and error scenarios

## Test Data

Test schemas used:
- **Simple User Schema**: Basic object with name, age, email
- **Person Schema**: Complex nested structure with address, employment
- **Product Schema**: Schema with enum values
- **Invalid Schemas**: Malformed JSON for error testing

## Assertions

### HTTP Status Codes
- `201 Created` - Schema saved successfully
- `200 OK` - Validation, Avro conversion, JAR generation successful
- `400 Bad Request` - Invalid input, schema not found
- `500 Internal Server Error` - Unexpected errors

### Response Validation
- JSON structure validation
- Field value verification
- Error message content
- Content-Type headers
- Content-Disposition headers for downloads

## CI/CD Integration

Tests are designed to run in CI/CD pipelines:
- No external dependencies required
- Uses in-memory database
- Fast execution (< 30 seconds)
- Comprehensive coverage of happy paths and error scenarios
- Clear test output with descriptive names

## Future Test Improvements

Potential areas for expansion:
- [ ] Performance tests for large schemas
- [ ] Concurrency tests for parallel requests
- [ ] Load tests with JMeter or Gatling
- [ ] Contract tests with Pact
- [ ] Mutation testing with PIT
- [ ] Security tests for input validation
- [ ] End-to-end tests with real JAR extraction
