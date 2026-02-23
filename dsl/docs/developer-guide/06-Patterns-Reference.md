# Patterns Reference

This chapter documents the design patterns, naming conventions, and key utilities used throughout the Nebula DSL codebase.

## Design Patterns

### Pattern Catalog

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Template Method** | `MethodGeneratorTemplate`, CRUD generators | Define algorithm skeleton |
| **Strategy** | DTO setter builders (`dto-setter-strategies/`) | Select algorithm at runtime |
| **Facade** | Feature facades (`src/cli/features/`) | Simplify complex interactions |
| **Builder** | Context builders (`event-context-builder.ts`) | Construct complex objects step by step |
| **Registry** | `GeneratorRegistry`, Model Registry, DTO Schema Registry | Central lookup and management |
| **Singleton** | `TemplateManager` | Single instance coordination |
| **Factory** | `GeneratorRegistryFactory` | Create related objects |

### Template Method Pattern

**Where:** `MethodGeneratorTemplate` -- base class for CRUD method generators.

**How it works:** The base class defines the algorithm skeleton (`generate()`), subclasses override specific steps (`extractMetadata`, `buildMethodSignature`, `buildMethodBody`).

```typescript
// Base class defines structure
generate(aggregate, options) {
    const metadata = this.extractMetadata(aggregate, options);  // Abstract
    const signature = this.buildMethodSignature(metadata);       // Abstract
    const body = this.buildMethodBody(metadata);                 // Abstract
    const errorHandling = this.buildErrorHandling(metadata);     // Hook (overridable)
    return this.assembleMethod(signature, body, errorHandling);
}
```

**Subclasses:** `CrudCreateGenerator`, `CrudReadGenerator`, `CrudUpdateGenerator`, `CrudDeleteGenerator`

### Strategy Pattern

**Where:** DTO setter builders -- different strategies for different field types.

```typescript
export interface DtoSetterStrategy {
    canHandle(field: Property, entity: Entity): boolean;
    buildSetter(field: Property, entity: Entity, context: Context): string;
}

// Strategies (checked in order, first match wins):
// 1. AggregateFieldStrategy  -- cross-aggregate fields
// 2. EnumFieldStrategy       -- enum fields
// 3. OverrideFieldStrategy   -- field mappings
// 4. ExtractFieldStrategy    -- extract patterns
// 5. DefaultSetterStrategy   -- fallback for simple fields
```

**Usage:**
```typescript
const builder = new DtoSetterBuilder();
for (const field of entity.properties) {
    const setter = builder.buildSetter(field, entity, context);
}
```

### Facade Pattern

**Where:** Feature facades coordinate multiple generators.

```typescript
// Instead of calling 5 generators manually:
await EntityFeature.generateCoreComponents(aggregate, path, options, generators);
```

Each facade (EntityFeature, ServiceFeature, etc.) encapsulates the coordination of related generators, file writing, and error handling.

### Builder Pattern

**Where:** Context builders for templates.

```typescript
const context = new EventContextBuilder()
    .withAggregate(aggregate)
    .withEvent(event)
    .withOptions(options)
    .withImports(['java.util.*'])
    .build();  // Validates and returns
```

### Registry Pattern

**Where:** `GeneratorRegistry` (generator instances), Model Registry (cross-file aggregates), DTO Schema Registry.

Provides centralized lookup without passing objects through many layers.

### Singleton Pattern

**Where:** `TemplateManager` -- ensures template cache is shared across all generators.

```typescript
const manager = TemplateManager.getInstance();
// Always returns the same instance
```

### Pattern Selection Guide

| Use Case | Pattern |
|----------|---------|
| Fixed algorithm, variable steps | **Template Method** |
| Multiple algorithms for same task | **Strategy** |
| Coordinate multiple subsystems | **Facade** |
| Complex object construction | **Builder** |
| Global object lookup | **Registry** |
| Single shared instance | **Singleton** |

## Naming Conventions

### Aggregate Name Variables

```typescript
// CORRECT PATTERN
const aggregateName = aggregate.name;                           // Original case: "User"
const lowerAggregate = aggregateName.toLowerCase();             // Lowercase: "user"
const capitalizedAggregate = StringUtils.capitalize(aggregateName);  // Capitalized: "User"

// INCORRECT PATTERN
const aggregateName = aggregate.name.toLowerCase();  // WRONG - shadows original case
const name = aggregate.name;                         // WRONG - too generic
```

### Standard Variable Names

| Variable Name | Purpose | Example Value |
|--------------|---------|---------------|
| `aggregateName` | Original aggregate name (from AST) | `"User"`, `"Quiz"` |
| `lowerAggregate` | Lowercase (for packages/paths) | `"user"`, `"quiz"` |
| `capitalizedAggregate` | Capitalized (for class names) | `"User"`, `"Quiz"` |
| `entityName` | Original entity name | `"User"`, `"Course"` |
| `lowerEntity` | Lowercase entity name | `"user"`, `"course"` |
| `projectName` | Original project name | `"answers"` |
| `lowerProject` | Lowercase project name | `"answers"` |

### Generator Method Template

```typescript
function generateSomething(aggregate: Aggregate, options: GenerationOptions): string {
    // 1. Extract names with original case
    const aggregateName = aggregate.name;
    const projectName = options.projectName;

    // 2. Create lowercase variants for packages/paths
    const lowerAggregate = aggregateName.toLowerCase();
    const lowerProject = projectName.toLowerCase();

    // 3. Create capitalized variants for class names
    const capitalizedAggregate = StringUtils.capitalize(aggregateName);

    // 4. Use variables consistently
    const packageName = `${basePackage}.${lowerProject}.microservices.${lowerAggregate}`;
    const className = `${capitalizedAggregate}Service`;

    return template;
}
```

### File Naming Conventions

| File Type | Convention | Example |
|-----------|-----------|---------|
| Generators | `*-generator.ts` | `entity-generator.ts` |
| Features | `*-feature.ts` | `entity-feature.ts` |
| Utilities | `*-helpers.ts`, `*-utils.ts` | `aggregate-helpers.ts` |
| Builders | `*-builder.ts` | `event-context-builder.ts` |
| Templates | `kebab-case.hbs` | `factory-interface.hbs` |

## Key Utilities Reference

### `aggregate-helpers.ts`

Core aggregate metadata extraction:

```typescript
// Find root entity in aggregate
findRootEntity(aggregate: Aggregate): Entity

// Get all entities (root + child)
getEntities(aggregate: Aggregate): Entity[]

// Resolve type from referenced aggregate (cross-file)
resolveTypeFromReferencedAggregate(entity: Entity, dtoField: string): string

// Global model registry
registerAllModels(models: Model[]): void
findAggregateInModels(name: string): Aggregate | undefined

// DTO field to string conversion
dtoFieldToString(dtoField: DtoFieldPath | string): string
```

### `string-utils.ts`

String manipulation utilities:

```typescript
capitalize(str: string): string       // "user" → "User"
lowercase(str: string): string        // "User" → "user"
toCamelCase(str: string): string      // "user_name" → "userName"
toKebabCase(str: string): string      // "userName" → "user-name"
```

### `package-name-builder.ts`

Java package name construction:

```typescript
buildPackageName(projectName: string, ...segments: string[]): string
// e.g., "pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate"
```

### `import-manager.ts`

Manages Java imports for generated code:

```typescript
addImport(importStatement: string): void
getImports(): string[]
combineImports(...importSets: string[][]): string[]
```

### `file-writer.ts`

File I/O for generated code:

```typescript
static async writeGeneratedFile(
    filePath: string,
    content: string,
    description?: string
): Promise<void>
```

### `error-handler.ts`

Contextual error handling:

```typescript
static handle(
    error: Error,
    context: ErrorContext,
    severity: ErrorSeverity
): void

static async wrapAsync(
    fn: () => Promise<void>,
    context: ErrorContext,
    severity: ErrorSeverity
): Promise<void>
```

### `UnifiedTypeResolver`

Type resolution from AST to Java types:

```typescript
resolveJavaType(type: Type): string        // AST type → "String", "Set<Integer>", etc.
isCollectionType(type: Type): boolean      // List<T> or Set<T>
isEntityType(type: Type): boolean          // Entity or enum reference
getElementType(type: Type): string         // Collection element type
```

### `dto-schema-service.ts`

DTO schema registry for cross-aggregate DTO references:

```typescript
buildFromModels(models: Model[]): DtoSchemaRegistry
getSchema(entityName: string): DtoSchema | undefined
```

## Cross-References Between Guides

| Topic | Lives in | Referenced from |
|-------|----------|-----------------|
| DSL syntax & constructs | [User Guide: DSL Syntax](../user-guide/03-DSL-Syntax.md) | Grammar Internals |
| Grammar reference | [User Guide: Reference](../user-guide/05-Reference.md) | Grammar Internals |
| Generated code structure | [User Guide: Generated Code](../user-guide/04-Generated-Code.md) | Generator System |
| Architecture & pipeline | [Dev Guide: Architecture](01-Architecture.md) | User Guide Introduction |
| Generator details | [Dev Guide: Generator System](03-Generator-System.md) | User Guide Generated Code |
| Adding features | [Dev Guide: Adding Features](05-Adding-Features.md) | User Guide Introduction |

---

**Previous:** [05-Adding-Features](05-Adding-Features.md) | **Back to:** [01-Architecture](01-Architecture.md)
