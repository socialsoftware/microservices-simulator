# Nebula DSL Generator - Refactoring Plan

## Overview
This document outlines the refactoring plan for the Nebula DSL generator codebase to improve code quality, remove technical debt, and ensure all generation is dynamic and maintainable.

## 1. Code Cleanup Tasks

### 1.1 Remove Debug Statements
- [x] Remove all `console.log` debug statements from TypeScript files
  - `type-resolver.ts`: Debug statements for ListType, CollectionType, PrimitiveType
  - `shared-dto-generator.ts`: Any remaining debug logs
  - `entity-generator.ts`: Debug logs for type resolution
  - `service-definition-generator.ts`: Debug logs for business logic processing

### 1.2 Remove Unnecessary Comments
- [ ] Remove outdated TODO comments
- [ ] Remove commented-out code blocks
- [ ] Remove obvious comments that don't add value
- [ ] Keep only essential documentation comments

### 1.3 Remove Dead Code
- [ ] Remove unused imports in all TypeScript files
- [ ] Remove unused methods and functions
- [ ] Remove legacy/deprecated code paths
- [ ] Remove hardcoded fallbacks that are no longer needed

## 2. Code Organization & Structure

### 2.1 File Structure Review
Current structure assessment:
```
src/cli/
├── core/                    # Core functionality (scope, validation)
├── generator/              
│   ├── base/               # Base classes and utilities
│   └── impl/               # Implementation generators
│       ├── config/         # Configuration generators
│       ├── coordination/   # Coordination & functionalities
│       ├── entity/         # Entity & factory generators
│       ├── events/         # Event generators
│       ├── repository/     # Repository generators
│       ├── saga/          # Saga-specific generators
│       ├── service/       # Service generators
│       ├── shared/        # Shared DTOs & enums
│       └── web/           # Web/API generators
├── orchestration/          # Feature orchestration
└── utils/                  # Utilities
```

**Proposed improvements:**
- [ ] Create `types/` directory for TypeScript type definitions
- [ ] Create `constants/` directory for shared constants
- [ ] Move template helpers to dedicated `template-helpers/` directory
- [ ] Consider merging similar generators (e.g., entity-related generators)

### 2.2 Template Organization
- [ ] Review and consolidate Handlebars templates
- [ ] Create shared partial templates for common patterns
- [ ] Remove duplicate template logic

## 3. Remove Hardcoded Logic

### 3.1 Type Resolution
- [ ] Remove hardcoded type mappings in `type-resolver.ts`
- [ ] Make all type resolution dynamic based on DSL definitions
- [ ] Remove special case handling that can be generalized

### 3.2 Import Generation
- [ ] Remove all hardcoded import lists
- [ ] Implement dynamic import detection based on actual usage
- [ ] Create a centralized import resolver

### 3.3 Entity Generation
- [ ] Remove hardcoded entity relationship logic
- [ ] Make collection initialization fully dynamic
- [ ] Remove hardcoded JPA annotation logic

### 3.4 DTO Generation
- [ ] Remove hardcoded DTO field mappings
- [ ] Make all DTO generation based on DSL definitions
- [ ] Remove special cases for specific DTOs

### 3.5 Service Generation
- [ ] Remove hardcoded service method patterns
- [ ] Make all business logic generation DSL-driven
- [ ] Remove hardcoded dependency injection

## 4. Code Simplification

### 4.1 Reduce Repetition
- [ ] Extract common patterns into reusable functions
- [ ] Create base generator class with common functionality
- [ ] Consolidate similar generation logic

### 4.2 Simplify Complex Methods
- [ ] Break down large methods into smaller, focused functions
- [ ] Simplify nested conditionals
- [ ] Extract complex logic into well-named helper methods

### 4.3 Type Safety Improvements
- [ ] Add proper TypeScript types for all DSL AST nodes
- [ ] Remove `any` types where possible
- [ ] Add type guards for runtime type checking

## 5. Specific Refactoring Tasks

### 5.1 TypeResolver Refactoring
- [ ] Consolidate type resolution logic into single method
- [ ] Remove duplicate primitive type checking
- [ ] Simplify collection type detection
- [ ] Make element type extraction more robust

### 5.2 EntityGenerator Refactoring
- [ ] Extract constructor generation into separate methods
- [ ] Simplify relationship detection logic
- [ ] Extract JPA annotation logic into helpers
- [ ] Make setter generation more dynamic

### 5.3 SharedDtoGenerator Refactoring
- [ ] Simplify field mapping logic
- [ ] Extract constructor generation
- [ ] Make collection handling more generic
- [ ] Remove special case logic

### 5.4 ServiceDefinitionGenerator Refactoring
- [ ] Extract business action processors into separate classes
- [ ] Simplify method implementation generation
- [ ] Make annotation generation dynamic
- [ ] Extract dependency resolution logic

### 5.5 EventGenerator Refactoring
- [ ] Consolidate event generation logic
- [ ] Remove duplicate template rendering
- [ ] Simplify event handler generation
- [ ] Make event subscription logic more dynamic

## 6. Testing & Validation

### 6.1 Add Unit Tests
- [ ] Create unit tests for TypeResolver
- [ ] Create unit tests for entity generation
- [ ] Create unit tests for DTO generation
- [ ] Create unit tests for service generation

### 6.2 Add Integration Tests
- [ ] Test complete aggregate generation
- [ ] Test cross-aggregate references
- [ ] Test event flow generation
- [ ] Test service method generation

### 6.3 Add Validation
- [ ] Enhance DSL validation rules
- [ ] Add generation-time validation
- [ ] Add post-generation validation

## 7. Documentation

### 7.1 Code Documentation
- [ ] Add JSDoc comments to all public methods
- [ ] Document complex algorithms
- [ ] Add examples in comments where helpful

### 7.2 Architecture Documentation
- [ ] Document the overall architecture
- [ ] Create generator workflow diagrams
- [ ] Document DSL-to-Java mapping rules

### 7.3 Usage Documentation
- [ ] Update README with current capabilities
- [ ] Add troubleshooting guide
- [ ] Create migration guide for DSL changes

## 8. Performance Optimization

### 8.1 Generation Performance
- [ ] Optimize file I/O operations
- [ ] Cache parsed DSL models
- [ ] Parallelize independent generation tasks
- [ ] Optimize template compilation

### 8.2 Memory Usage
- [ ] Review memory usage during generation
- [ ] Optimize large collection handling
- [ ] Clean up temporary objects

## 9. Error Handling

### 9.1 Improve Error Messages
- [ ] Add context to error messages
- [ ] Include DSL line numbers in errors
- [ ] Provide suggestions for fixing errors

### 9.2 Add Recovery Mechanisms
- [ ] Handle partial generation failures
- [ ] Add rollback capability
- [ ] Implement graceful degradation

## 10. Configuration & Extensibility

### 10.1 Configuration Management
- [ ] Make all hardcoded values configurable
- [ ] Support environment-specific configurations
- [ ] Add configuration validation

### 10.2 Plugin Architecture
- [ ] Design plugin interface for custom generators
- [ ] Support generator extensions
- [ ] Allow custom template registration

## Implementation Priority

### Phase 1: Critical Cleanup (Week 1)
1. Remove debug statements
2. Remove dead code
3. Fix critical hardcoding issues

### Phase 2: Core Refactoring (Week 2-3)
1. Refactor TypeResolver
2. Refactor EntityGenerator
3. Refactor SharedDtoGenerator

### Phase 3: Architecture Improvements (Week 4)
1. Reorganize file structure
2. Extract common base classes
3. Implement dynamic import resolution

### Phase 4: Testing & Documentation (Week 5)
1. Add unit tests
2. Add integration tests
3. Update documentation

### Phase 5: Advanced Features (Week 6+)
1. Performance optimization
2. Plugin architecture
3. Enhanced error handling

## Success Metrics

- **Code Quality**: Reduction in code duplication by 50%
- **Maintainability**: All generation logic is DSL-driven (0 hardcoded patterns)
- **Test Coverage**: Achieve 80% code coverage
- **Performance**: Generation time reduced by 30%
- **Documentation**: 100% of public APIs documented

## Notes

### Current Issues to Address
- CollectionType vs ListType/SetType confusion in grammar
- Inconsistent type resolution paths
- Hardcoded aggregate field handling
- Complex nested logic in generators
- Lack of separation between parsing and generation phases

### Risks
- Breaking existing DSL files during refactoring
- Performance regression during abstraction
- Increased complexity from over-engineering

### Dependencies
- Langium framework constraints
- Handlebars template engine limitations
- TypeScript version compatibility

## Conclusion

This refactoring plan aims to transform the Nebula DSL generator into a maintainable, extensible, and robust code generation framework. The phased approach ensures continuous functionality while gradually improving code quality.
