# Generator System

This chapter covers the generator architecture: the registry, feature facades, base classes, generator categories, and data flow.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Generator Registry                        │
│  Central registry of all generator instances                 │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Feature Facades                           │
│  EntityFeature, ServiceFeature, EventsFeature,               │
│  CoordinationFeature, WebApiFeature, SagaFeature             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Specialized Generators                    │
│  EntityOrchestrator, DtoGenerator, ServiceGenerator,         │
│  RepositoryGenerator, FactoryGenerator, EventGenerator, ...  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Generator Base Class                      │
│  Template rendering, type resolution, string utilities,      │
│  package naming, file I/O, error handling                    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Templates & Utilities                     │
│  Handlebars templates, TemplateManager, FileWriter,          │
│  UnifiedTypeResolver                                         │
└─────────────────────────────────────────────────────────────┘
```

## Generator Registry

**File:** `src/cli/engine/generator-registry.ts`

The registry is a centralized factory that creates and manages all generator instances:

```typescript
export interface GeneratorRegistry {
    entityGenerator: EntityOrchestrator;
    dtoGenerator: DtoGenerator;
    serviceGenerator: ServiceGenerator;
    repositoryGenerator: RepositoryGenerator;
    repositoryInterfaceGenerator: RepositoryInterfaceGenerator;
    factoryGenerator: FactoryGenerator;
    eventGenerator: EventGenerator;
    controllerGenerator: ControllerGenerator;
    sagaGenerator: SagaGenerator;
    configurationGenerator: ConfigurationGenerator;
    integrationGenerator: IntegrationGenerator;
    exceptionGenerator: ExceptionGenerator;
    validationSystem: ValidationSystem;
}

export class GeneratorRegistryFactory {
    static createRegistry(): GeneratorRegistry {
        return {
            entityGenerator: new EntityOrchestrator(),
            dtoGenerator: new DtoGenerator(),
            serviceGenerator: new ServiceGenerator(),
            // ... all generators
        };
    }
}
```

**Usage:**
```typescript
const generators = GeneratorRegistryFactory.createRegistry();
const entityCode = await generators.entityGenerator.generateEntity(entity, options);
```

## Feature Facades

Feature facades orchestrate multiple generators to produce a complete feature. Each facade:
- Coordinates related generators
- Handles file writing
- Isolates errors per feature
- Simplifies the main code generator

### EntityFeature

**File:** `src/cli/features/entity-feature.ts`

Generates entities, DTOs, factories, and repositories:

```typescript
export class EntityFeature {
    static async generateCoreComponents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        // 1. Generate entities
        for (const entity of aggregate.entities) {
            const entityCode = await generators.entityGenerator.generateEntity(entity, options);
            await FileWriter.writeGeneratedFile(entityPath, entityCode);
        }

        // 2. Generate DTOs
        // 3. Generate factory
        // 4. Generate repositories
    }
}
```

### Other Facades

| Facade | What It Generates |
|--------|-------------------|
| `EntityFeature` | Entities, DTOs, Factory, Repositories |
| `ServiceFeature` | Service class (CRUD + custom methods) |
| `EventsFeature` | Published events, subscriptions, handlers |
| `CoordinationFeature` | Functionalities, EventProcessing |
| `WebApiFeature` | REST controllers |
| `SagaFeature` | Saga workflows |
| `ValidationFeature` | Business rule validation |

## Generator Base Class

**File:** `src/cli/generators/common/base/generator-base.ts`

All generators extend `GeneratorBase`, which provides:

```typescript
export abstract class GeneratorBase {
    protected templateManager: TemplateManager;
    protected typeResolver: typeof UnifiedTypeResolver;

    // Template rendering
    protected renderTemplate(templatePath: string, context: any): string;

    // String utilities
    protected capitalize(str: string): string;
    protected lowercase(str: string): string;
    protected toCamelCase(str: string): string;

    // Aggregate helpers
    protected findRootEntity(aggregate: Aggregate): Entity;

    // Package naming
    protected generatePackageName(...segments: string[]): string;

    // Type resolution
    protected resolveJavaType(type: any): string;
    protected isCollectionType(type: any): boolean;
    protected isEntityType(type: any): boolean;

    // File I/O
    protected async writeFile(filePath: string, content: string): Promise<void>;

    // Utilities
    protected combineImports(...importSets: string[][]): string[];
    protected hasAnnotation(aggregate: Aggregate, name: string): boolean;
}
```

## Method Generator Template

**File:** `src/cli/generators/common/base/method-generator-template.ts`

A specialized base class for generators that create Java methods, using the Template Method pattern:

```typescript
export abstract class MethodGeneratorTemplate extends GeneratorBase {
    // Template method - defines algorithm skeleton
    generate(aggregate: Aggregate, options: GenerationOptions): string {
        const metadata = this.extractMetadata(aggregate, options);    // Step 1
        const signature = this.buildMethodSignature(metadata);        // Step 2
        const body = this.buildMethodBody(metadata);                  // Step 3
        const eventHandling = this.buildEventHandling(metadata);      // Step 4
        const errorHandling = this.buildErrorHandling(metadata);      // Step 5
        return this.assembleMethod(signature, body, eventHandling, errorHandling, metadata);
    }

    // Subclasses must implement:
    protected abstract extractMetadata(aggregate, options): MethodMetadata;
    protected abstract buildMethodSignature(metadata): string;
    protected abstract buildMethodBody(metadata): string;

    // Subclasses can override:
    protected buildEventHandling(metadata): string { return ''; }
    protected buildErrorHandling(metadata): string { /* default */ }
}
```

**Subclass example:**
```typescript
export class CrudCreateGenerator extends MethodGeneratorTemplate {
    protected extractMetadata(aggregate, options): MethodMetadata {
        return {
            methodName: `create${aggregate.name}`,
            aggregateName: aggregate.name,
            parameters: [
                { name: 'dto', type: `Create${aggregate.name}RequestDto` },
                { name: 'unitOfWork', type: 'UnitOfWork' }
            ],
            returnType: `${aggregate.name}Dto`
        };
    }
    // ... buildMethodSignature, buildMethodBody
}
```

## Generator Categories

### Microservices Generators

```
generators/microservices/
├── entity/
│   ├── entity-orchestrator.ts      # Main entity generator
│   ├── constructors.ts             # Constructor generation
│   ├── fields.ts                   # Field generation
│   ├── methods.ts                  # Getter/setter generation
│   ├── imports.ts                  # Import detection
│   ├── invariants.ts               # Invariant compilation
│   └── builders/
│       ├── class-assembler.ts
│       ├── dto-method-builder.ts
│       ├── event-subscription-builder.ts
│       ├── import-scanner.ts
│       └── dto-setter-strategies/  # Strategy pattern
├── repository/
│   ├── repository-generator.ts
│   ├── repository-interface-generator.ts
│   └── spring-data-query-parser.ts
├── service/
│   ├── default/
│   │   ├── crud-generator.ts
│   │   ├── crud-create-generator.ts
│   │   ├── crud-read-generator.ts
│   │   ├── crud-update-generator.ts
│   │   ├── crud-delete-generator.ts
│   │   └── collection-generator.ts
│   └── service-definition-generator.ts
├── factory/
│   └── factory-generator.ts
├── events/
│   ├── published-event-generator.ts
│   ├── event-handler-generator.ts
│   ├── event-subscription-generator.ts
│   └── references-generator.ts
└── shared/
    ├── dto-generator.ts
    ├── enum-generator.ts
    └── shared-feature.ts
```

### Coordination Generators

```
generators/coordination/
├── functionalities/
│   ├── functionalities-generator.ts
│   ├── functionalities-crud-generator.ts
│   └── functionalities-collection-generator.ts
├── webapi/
│   ├── webapi-generator.ts
│   ├── controller-generator.ts
│   ├── dto-generator.ts
│   └── endpoint-builder.ts
├── config/
│   ├── configuration-generator.ts
│   ├── application-config-generator.ts
│   └── integration-generator.ts
└── event-processing-generator.ts
```

### Saga Generators

```
generators/sagas/
├── saga-generator.ts                  # Saga aggregate/DTO/state/factory/repository
├── saga-functionality-generator.ts
├── saga-crud-generator.ts
├── saga-collection-generator.ts
├── saga-event-processing-generator.ts
├── saga-workflow-generator.ts
├── saga-helpers.ts
├── causal-entity-generator.ts
├── base/
│   └── saga-functionality-generator-base.ts
└── operations/
    ├── saga-create-generator.ts
    ├── saga-read-generator.ts
    ├── saga-read-all-generator.ts
    ├── saga-update-generator.ts
    └── saga-delete-generator.ts
```

## Data Flow Example

How a `.nebula` Entity becomes a Java class:

```
1. DSL Input:
   Root Entity User {
       String name
       UserRole role
   }

2. AST (after parsing):
   { $type: 'Entity', name: 'User', isRoot: true,
     properties: [
       { name: 'name', type: { typeName: 'String' } },
       { name: 'role', type: { type: { ref: UserRoleEnum } } }
     ]
   }

3. Generator (EntityOrchestrator):
   - Extract metadata from AST
   - Build fields, constructors, methods, invariants
   - Build template context

4. Template Context:
   { packageName: '...user.aggregate',
     className: 'User', extendsClause: 'extends Aggregate',
     fields: [{ type: 'String', name: 'name' }, ...],
     constructors: [...], methods: [...] }

5. Template Rendering → Java Output:
   package ...user.aggregate;
   @Entity
   public class User extends Aggregate {
       private String name;
       private UserRole role;
       // constructors, getters, setters, invariants
   }
```

## Error Handling Pattern

Generators use contextual error handling:

```typescript
try {
    const code = await generator.generateEntity(entity, options);
} catch (error) {
    ErrorHandler.handle(
        error,
        ErrorUtils.aggregateContext('generate entity', entity.name, 'entity-generator'),
        ErrorSeverity.FATAL
    );
}
```

For non-critical failures, use `ErrorSeverity.ERROR` to continue generation:

```typescript
await ErrorHandler.wrapAsync(
    async () => { /* generation code */ },
    ErrorUtils.aggregateContext('generate service', aggregate.name, 'service-generator'),
    ErrorSeverity.ERROR  // Don't stop on service generation failure
);
```

## `GenerationOptions` Interface

Passed to all generators:

```typescript
export interface GenerationOptions {
    projectName: string;
    basePackage: string;
    outputDirectory: string;
    consistencyModels: string[];
}
```

> For details on what the generators produce, see the [User Guide](../user-guide/04-Generated-Code.md).

---

**Previous:** [02-Grammar-Internals](02-Grammar-Internals.md) | **Next:** [04-Template-System](04-Template-System.md)
