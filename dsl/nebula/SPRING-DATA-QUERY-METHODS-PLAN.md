# Spring Data JPA Query Methods Implementation Plan

## Overview
Add support for both manual queries (`@Query`) and derived queries (method name-based) in the Nebula DSL, with validation and autocompletion support.

## Goals
1. Support both query definition approaches:
   - **Manual queries**: Using `@Query("...")` annotation (already supported)
   - **Derived queries**: Method name-based queries (new feature)
2. Provide validation for derived query method names
3. Provide autocompletion support for developers
4. Generate correct Java code for both approaches

## Current State

### Grammar
- `RepositoryMethod` already supports optional `@Query` annotation
- Grammar: `('@Query' '(' query=STRING ')')? returnType=RepositoryReturnType name=ID ...`

### Code Generation
- Repository generator currently only handles methods with `@Query`
- Methods without `@Query` are ignored or have fallback logic

## Implementation Plan

### Phase 1: Query Method Name Parser & Validator

#### 1.1 Create Query Method Parser Utility
**File**: `src/cli/utils/query-method-parser.ts`

**Purpose**: Parse Spring Data JPA method names and extract:
- Query prefix (`find`, `exists`, `count`, `delete`, `remove`)
- Property names referenced in the method
- Keywords used (`And`, `Or`, `Between`, `LessThan`, etc.)
- Sorting (`OrderBy...Asc/Desc`)
- Limiting (`First`, `Top`)

**Functions**:
```typescript
interface ParsedQueryMethod {
  prefix: 'find' | 'exists' | 'count' | 'delete' | 'remove' | null;
  properties: string[];
  keywords: string[];
  sortFields: string[];
  limit?: number;
  isValid: boolean;
  errors: string[];
}

function parseQueryMethodName(methodName: string): ParsedQueryMethod
function validatePropertyExists(propertyName: string, entity: Entity): boolean
function validateKeywordSequence(keywords: string[]): boolean
```

**Supported Prefixes**:
- `findBy`, `findAllBy`, `findFirstBy`, `findTopBy`
- `existsBy`
- `countBy`
- `deleteBy`, `removeBy`

**Supported Keywords**:
- Logical: `And`, `Or`, `Not`
- Comparison: `Is`, `Equals`, `Between`, `LessThan`, `LessThanEqual`, `GreaterThan`, `GreaterThanEqual`, `Before`, `After`
- String: `Like`, `NotLike`, `StartingWith`, `EndingWith`, `Containing`, `IgnoreCase`
- Null: `IsNull`, `IsNotNull`, `Null`, `NotNull`
- Collection: `In`, `NotIn`
- Boolean: `True`, `False`
- Sorting: `OrderBy...Asc`, `OrderBy...Desc`
- Limiting: `First`, `Top` (with number)

#### 1.2 Add Validation for Repository Methods
**File**: `src/language/nebula-validator.ts`

**Changes**:
1. Register `RepositoryMethod` validation in `registerValidationChecks`
2. Add `checkRepositoryMethod` method to `NebulaValidator` class

**Validation Rules**:
- If method has `@Query`: Validate query syntax (optional, basic check)
- If method has no `@Query`: Validate as derived query
  - Check method name follows Spring Data pattern
  - Validate all property names exist on root entity
  - Validate keyword sequence is valid
  - Validate parameter count matches property/keyword usage
  - Validate return type is appropriate for prefix
  - Check for nested properties (e.g., `findByUserEmail` where `user` is a relation)

**Error Messages**:
- `Property '${propertyName}' not found on entity '${entityName}'`
- `Invalid query method name: '${methodName}'. Expected pattern: findBy...`
- `Keyword '${keyword}' cannot follow '${previousKeyword}'`
- `Parameter count mismatch: expected ${expected}, got ${actual}`
- `Return type '${returnType}' is not valid for prefix '${prefix}'`

### Phase 2: Autocompletion Support

#### 2.1 Enhance Completion Provider
**File**: `src/language/completion-provider.ts`

**New Features**:
1. **Context-aware completions** for `CustomRepository` block
2. **Method prefix suggestions**: When typing method name, suggest:
   - `findBy`
   - `findAllBy`
   - `existsBy`
   - `countBy`
   - `deleteBy`
   - `removeBy`
   - `findFirstBy`, `findTopBy`

3. **Property name suggestions**: After `findBy`, suggest:
   - All properties from root entity
   - Nested properties (e.g., `user.email` if `user` is a relation)

4. **Keyword suggestions**: After property name, suggest:
   - `And`, `Or`
   - `Is`, `Equals`
   - `Like`, `Containing`, `StartingWith`, `EndingWith`
   - `LessThan`, `GreaterThan`, `Between`
   - `In`, `NotIn`
   - `IsNull`, `IsNotNull`
   - `True`, `False`
   - `IgnoreCase`

5. **Sorting suggestions**: After query conditions, suggest:
   - `OrderBy${Property}Asc`
   - `OrderBy${Property}Desc`

6. **Return type suggestions**: Based on prefix:
   - `findBy` → `Optional<T>`, `List<T>`, `Set<T>`, `T`, `Page<T>`, `Slice<T>`
   - `existsBy` → `Boolean`
   - `countBy` → `Long`, `Integer`
   - `deleteBy` → `void`, `Long` (count)

**Implementation**:
- Use Langium's completion context to detect position
- Check if cursor is in `CustomRepository` block
- Check if cursor is in method name, parameter list, or return type
- Provide appropriate suggestions based on context

### Phase 3: Code Generation Updates

#### 3.1 Update Repository Interface Generator
**File**: `src/cli/generators/microservices/repository/repository-interface-generator.ts`

**Changes**:
1. **Method Detection**:
   - Check if method has `query` property (from `@Query`)
   - If yes: Generate with `@Query` annotation (existing behavior)
   - If no: Generate without `@Query` (Spring Data will derive it)

2. **Query Generation**:
   ```typescript
   if (method.query && method.query.trim() !== '') {
       // Manual query - add @Query annotation
       return `    @Query(value = "${method.query}")
    ${method.returnType} ${method.name}(...);`;
   } else {
       // Derived query - no @Query annotation
       return `    ${method.returnType} ${method.name}(...);`;
   }
   ```

3. **Import Management**:
   - Only import `@Query` if at least one method uses manual query
   - (Already implemented in previous changes)

#### 3.2 Update Repository Method Builder
**File**: `src/cli/generators/microservices/repository/repository-interface-generator.ts`

**Method**: `addCustomRepositoryMethods`

**Changes**:
- Remove fallback query generation for methods without `@Query`
- Let Spring Data handle derived queries automatically
- Keep existing logic for methods with `@Query`

### Phase 4: Testing & Documentation

#### 4.1 Test Cases

**Derived Query Examples**:
```nebula
CustomRepository {
    // Simple property lookup
    Optional<Course> findByName(String name);
    
    // Multiple conditions
    List<Course> findByTypeAndActive(CourseType type, Boolean active);
    
    // String operations
    List<Course> findByNameContaining(String name);
    List<Course> findByNameStartingWith(String prefix);
    
    // Comparison
    List<Course> findByCreationDateAfter(LocalDateTime date);
    List<Course> findByAgeBetween(Integer min, Integer max);
    
    // Null checks
    List<Course> findByDescriptionIsNotNull();
    
    // Collections
    List<Course> findByTypeIn(List<CourseType> types);
    
    // Sorting
    List<Course> findByTypeOrderByNameAsc(CourseType type);
    
    // Limiting
    Course findFirstByTypeOrderByCreationDateDesc(CourseType type);
    List<Course> findTop10ByActiveTrue();
    
    // Exists/Count
    Boolean existsByName(String name);
    Long countByType(CourseType type);
    
    // Delete
    void deleteByType(CourseType type);
    Long removeByActiveFalse();
}
```

**Mixed Examples** (Manual + Derived):
```nebula
CustomRepository {
    // Derived query
    Optional<Course> findByName(String name);
    
    // Manual query
    @Query("SELECT c FROM Course c WHERE c.type = :type AND c.active = true")
    List<Course> findActiveCoursesByType(CourseType type);
    
    // Another derived query
    Boolean existsByName(String name);
}
```

#### 4.2 Validation Test Cases

**Should Pass**:
- `findByName(String name)` - valid property
- `findByTypeAndActive(CourseType type, Boolean active)` - valid properties
- `findByUserEmail(String email)` - nested property (if `user` relation exists)

**Should Fail**:
- `findByInvalidProperty(String value)` - property doesn't exist
- `findBy` - incomplete method name
- `findByTypeAnd` - incomplete keyword
- `findByType(String type, String extra)` - parameter count mismatch
- `existsByName(String name): List<Course>` - invalid return type for `exists`

#### 4.3 Documentation Updates

**Files to Update**:
- `README.md` - Add section on query methods
- Create `QUERY-METHODS.md` - Comprehensive guide

**Content**:
- Explanation of both approaches
- Examples of derived queries
- Supported keywords reference
- Validation rules
- Best practices

## Implementation Order

1. **Phase 1.1**: Create query method parser utility
2. **Phase 1.2**: Add validation for repository methods
3. **Phase 3**: Update code generation (can be done in parallel)
4. **Phase 2**: Add autocompletion support
5. **Phase 4**: Testing and documentation

## Files to Create/Modify

### New Files
- `src/cli/utils/query-method-parser.ts` - Parser and validator utilities
- `QUERY-METHODS.md` - Documentation

### Modified Files
- `src/language/nebula-validator.ts` - Add RepositoryMethod validation
- `src/language/completion-provider.ts` - Add autocompletion for query methods
- `src/cli/generators/microservices/repository/repository-interface-generator.ts` - Update code generation

## Success Criteria

1. ✅ Developers can write derived query methods without `@Query`
2. ✅ Validation catches invalid method names and property references
3. ✅ Autocompletion suggests valid prefixes, properties, and keywords
4. ✅ Generated Java code works correctly with Spring Data JPA
5. ✅ Both manual and derived queries work in the same repository
6. ✅ Clear error messages guide developers to fix issues

## Future Enhancements (Out of Scope)

- Support for projections (DTOs from queries)
- Support for `@Modifying` queries
- Support for native queries (`@Query(nativeQuery = true)`)
- Support for pagination parameters (`Pageable`)
- Support for `@EntityGraph` for eager loading
- Support for `Specification` API
- Support for `Example` queries

## References

- [Spring Data JPA Query Methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)
- [Baeldung: Spring Data Derived Queries](https://www.baeldung.com/spring-data-derived-queries)
- [Spring Data Repository Query Keywords](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#appendix.query.method.subject)

