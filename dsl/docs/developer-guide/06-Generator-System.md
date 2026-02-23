# Generator System

This chapter explores Nebula's generator architecture, including the generator registry, base classes, capabilities system, and design patterns used throughout the codebase.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Generator Registry                       │
│  • Central registry of all generators                       │
│  • Dependency management                                    │
│  • Lazy initialization                                      │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Feature Facades                          │
│  • EntityFeature    • ServiceFeature                        │
│  • EventsFeature    • WebApiFeature                         │
│  • SagaFeature      • CoordinationFeature                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Specialized Generators                   │
│  • EntityGenerator   • DtoGenerator                         │
│  • ServiceGenerator  • RepositoryGenerator                  │
│  • EventGenerator    • ControllerGenerator                  │
│  • SagaGenerator     • FactoryGenerator                     │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Generator Base Class                     │
│  • Template rendering   • Type resolution                   │
│  • String utilities     • Package naming                    │
│  • File I/O             • Error handling                    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Templates & Utilities                    │
│  • Handlebars templates                                     │
│  • Template Manager                                         │
│  • File Writer                                              │
│  • Type Resolver                                            │
└─────────────────────────────────────────────────────────────┘
```

## Generator Registry

### Purpose

The `GeneratorRegistry` is a centralized factory that:
- Creates and manages all generator instances
- Resolves generator dependencies
- Provides type-safe access to generators
- Enables lazy initialization

### Implementation

**File:** `src/cli/engine/generator-registry.ts`

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
            repositoryGenerator: new RepositoryGenerator(),
            repositoryInterfaceGenerator: new RepositoryInterfaceGenerator(),
            factoryGenerator: new FactoryGenerator(),
            eventGenerator: new EventGenerator(),
            controllerGenerator: new ControllerGenerator(),
            sagaGenerator: new SagaGenerator(),
            configurationGenerator: new ConfigurationGenerator(),
            integrationGenerator: new IntegrationGenerator(),
            exceptionGenerator: new ExceptionGenerator(),
            validationSystem: new ValidationSystem()
        };
    }
}
```

### Usage

```typescript
// In code-generator.ts
const generators = GeneratorRegistryFactory.createRegistry();

// Type-safe access
const entityCode = await generators.entityGenerator.generateEntity(entity, options);
const dtoCode = await generators.dtoGenerator.generateDto(entity, options);
```

## Generator Base Class

### Overview

`GeneratorBase` provides common functionality to all generators.

**File:** `src/cli/generators/common/base/generator-base.ts`

### Core Capabilities

```typescript
export abstract class GeneratorBase {
    protected templateManager: TemplateManager;
    protected typeResolver: typeof UnifiedTypeResolver;

    constructor(protected options?: GeneratorOptions) {
        this.templateManager = TemplateManager.getInstance();
        this.typeResolver = UnifiedTypeResolver;
    }

    // 1. Template Rendering
    protected renderTemplate(templatePath: string, context: any): string;

    // 2. String Utilities
    protected capitalize(str: string): string;
    protected lowercase(str: string): string;
    protected uppercase(str: string): string;
    protected toCamelCase(str: string): string;
    protected toKebabCase(str: string): string;
    protected toSnakeCase(str: string): string;

    // 3. Aggregate Helpers
    protected findRootEntity(aggregate: Aggregate): Entity;
    protected createAggregateNaming(aggregateName: string): NamingConventions;

    // 4. Package Naming
    protected getBasePackage(): string;
    protected generatePackageName(...segments: string[]): string;
    protected buildPackageName(...segments: string[]): string;

    // 5. Type Resolution
    protected resolveJavaType(type: any): string;
    protected isCollectionType(type: any): boolean;
    protected isEntityType(type: any): boolean;
    protected getElementType(type: any): string | undefined;

    // 6. File I/O
    protected async writeFile(filePath: string, content: string): Promise<void>;
    protected async ensureDirectory(dirPath: string): Promise<void>;
    protected async fileExists(filePath: string): Promise<boolean>;
    protected async readFile(filePath: string): Promise<string>;

    // 7. Utilities
    protected combineImports(...importSets: string[][]): string[];
    protected hasAnnotation(aggregate: Aggregate, name: string): boolean;
    protected getProjectName(): string;
}
```

### Example Usage

```typescript
export class EntityGenerator extends GeneratorBase {
    async generateEntity(entity: Entity, options: GenerationOptions): Promise<string> {
        // Use base class capabilities
        const packageName = this.generatePackageName(
            options.projectName,
            entity.name.toLowerCase(),
            'aggregate'
        );

        const className = this.capitalize(entity.name);
        const imports = this.buildImports(entity);

        const context = {
            packageName,
            className,
            imports,
            fields: this.buildFields(entity)
        };

        return this.renderTemplate('entity/entity.hbs', context);
    }

    private buildFields(entity: Entity): string[] {
        return entity.properties.map(prop => {
            const type = this.resolveJavaType(prop.type);
            const name = prop.name;
            return `private ${type} ${name};`;
        });
    }
}
```

## Method Generator Template

### Overview

`MethodGeneratorTemplate` is a specialized base class for generators that create Java methods using the Template Method pattern.

**File:** `src/cli/generators/common/base/method-generator-template.ts`

### Template Method Pattern

```typescript
export abstract class MethodGeneratorTemplate extends GeneratorBase {

    generate(aggregate: Aggregate, options: GenerationOptions): string {
        // 1. Extract metadata
        const metadata = this.extractMetadata(aggregate, options);

        // 2. Build method signature
        const signature = this.buildMethodSignature(metadata);

        // 3. Build method body
        const body = this.buildMethodBody(metadata);

        // 4. Build event handling (optional)
        const eventHandling = this.buildEventHandling(metadata);

        // 5. Build error handling (optional)
        const errorHandling = this.buildErrorHandling(metadata);

        // 6. Assemble complete method
        return this.assembleMethod(signature, body, eventHandling, errorHandling, metadata);
    }

    // Abstract methods - subclasses must implement
    protected abstract extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata;
    protected abstract buildMethodSignature(metadata: MethodMetadata): string;
    protected abstract buildMethodBody(metadata: MethodMetadata): string;

    // Optional hooks - subclasses can override
    protected buildEventHandling(metadata: MethodMetadata): string {
        return '';
    }

    protected buildErrorHandling(metadata: MethodMetadata): string {
        const action = this.extractActionFromMethodName(metadata.methodName);
        const entityName = this.lowercase(metadata.aggregateName);

        return ExceptionGenerator.generateCatchBlock(
            metadata.projectName,
            action,
            entityName
        );
    }

    protected assembleMethod(...parts: any[]): string {
        // Default assembly logic
    }
}
```

### Example Subclass

```typescript
export class CrudCreateGenerator extends MethodGeneratorTemplate {
    protected extractMetadata(aggregate: Aggregate, options: GenerationOptions): MethodMetadata {
        return {
            methodName: `create${aggregate.name}`,
            aggregateName: aggregate.name,
            entityName: aggregate.name,
            projectName: options.projectName,
            parameters: [
                { name: 'dto', type: `Create${aggregate.name}RequestDto` },
                { name: 'unitOfWork', type: 'UnitOfWork' }
            ],
            returnType: `${aggregate.name}Dto`
        };
    }

    protected buildMethodSignature(metadata: MethodMetadata): string {
        const params = this.buildParameterList(metadata.parameters);
        return `public ${metadata.returnType} ${metadata.methodName}(${params})`;
    }

    protected buildMethodBody(metadata: MethodMetadata): string {
        return `
            ${metadata.aggregateName} ${this.lowercase(metadata.aggregateName)} =
                ${this.lowercase(metadata.aggregateName)}Factory.create${metadata.aggregateName}(null, dto);
            ${this.lowercase(metadata.aggregateName)} =
                unitOfWorkService.registerChanged(${this.lowercase(metadata.aggregateName)}, unitOfWork);
            return ${this.lowercase(metadata.aggregateName)}Factory.create${metadata.aggregateName}Dto(${this.lowercase(metadata.aggregateName)});
        `;
    }
}
```

## Feature Facades

### Purpose

Feature facades orchestrate multiple generators to produce a complete feature.

### EntityFeature Example

**File:** `src/cli/features/entity-feature.ts`

```typescript
export class EntityFeature {
    static async generateCoreComponents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        // 1. Generate all entities
        for (const entity of aggregate.entities) {
            const entityCode = await generators.entityGenerator.generateEntity(entity, options);
            const entityPath = path.join(aggregatePath, 'aggregate', `${entity.name}.java`);
            await FileWriter.writeGeneratedFile(entityPath, entityCode, `entity ${entity.name}`);

            // 2. Generate DTOs
            const dtoCode = await generators.dtoGenerator.generateDto(entity, options);
            const dtoPath = path.join(aggregatePath, '..', '..', 'shared', 'dtos', `${entity.name}Dto.java`);
            await FileWriter.writeGeneratedFile(dtoPath, dtoCode, `DTO ${entity.name}Dto`);
        }

        // 3. Generate factory
        const factoryCode = await generators.factoryGenerator.generateFactory(aggregate, options);
        const factoryPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}Factory.java`);
        await FileWriter.writeGeneratedFile(factoryPath, factoryCode, `factory ${aggregate.name}Factory`);

        // 4. Generate repositories
        const repositoryCode = await generators.repositoryGenerator.generateRepository(aggregate, options);
        const repositoryPath = path.join(aggregatePath, 'aggregate', `${aggregate.name}CustomRepository.java`);
        await FileWriter.writeGeneratedFile(repositoryPath, repositoryCode, `custom repository`);

        const repositoryInterfaceCode = await generators.repositoryInterfaceGenerator.generateRepositoryInterface(aggregate, options);
        const repositoryInterfacePath = path.join(aggregatePath, 'aggregate', `${aggregate.name}Repository.java`);
        await FileWriter.writeGeneratedFile(repositoryInterfacePath, repositoryInterfaceCode, `repository interface`);

        // 5. Generate base service if no custom service defined
        const hasServiceDefinition = (aggregate as any).serviceDefinition;
        if (!hasServiceDefinition) {
            const serviceCode = await generators.serviceGenerator.generateService(aggregate, options);
            const servicePath = path.join(aggregatePath, 'service', `${aggregate.name}Service.java`);
            await FileWriter.writeGeneratedFile(servicePath, serviceCode, `service ${aggregate.name}Service`);
        }
    }
}
```

### Benefits of Facades

1. **Single responsibility** - Each facade handles one feature
2. **Coordinated generation** - Ensures all related files are generated together
3. **Error isolation** - Failures in one feature don't affect others
4. **Simplified main generator** - CodeGenerator delegates to facades
5. **Testability** - Each facade can be tested independently

## Generator Capabilities

### Capability System

Generators declare their capabilities through interfaces:

```typescript
export interface GeneratorCapabilities {
    // What can this generator produce?
    canGenerateEntity?: boolean;
    canGenerateDto?: boolean;
    canGenerateService?: boolean;
    canGenerateRepository?: boolean;

    // What inputs does it need?
    requiresRootEntity?: boolean;
    requiresEvents?: boolean;
    requiresInvariants?: boolean;

    // What features does it support?
    supportsCollections?: boolean;
    supportsCrossAggregateReferences?: boolean;
    supportsEventPublishing?: boolean;
    supportsEventSubscription?: boolean;
}
```

### Example Implementation

```typescript
export class EntityGenerator implements GeneratorCapabilities {
    canGenerateEntity = true;
    requiresRootEntity = false;  // Can generate non-root entities too
    supportsCollections = true;
    supportsCrossAggregateReferences = true;

    async generateEntity(entity: Entity, options: GenerationOptions): Promise<string> {
        // Check capabilities
        if (entity.isRoot && !this.requiresRootEntity) {
            // Special handling for root entity
        }

        if (this.hasCollectionProperties(entity) && !this.supportsCollections) {
            throw new Error('Generator does not support collections');
        }

        // Generate
    }
}
```

## Design Patterns in Generators

### 1. Template Method Pattern

**Where:** `MethodGeneratorTemplate`

**Purpose:** Define algorithm skeleton, let subclasses fill in steps.

**Example:**
```typescript
// Template method
generate(aggregate, options) {
    const metadata = this.extractMetadata(aggregate, options);  // Step 1
    const signature = this.buildMethodSignature(metadata);      // Step 2
    const body = this.buildMethodBody(metadata);                // Step 3
    return this.assembleMethod(signature, body, metadata);      // Step 4
}

// Subclasses override specific steps
protected abstract extractMetadata(...): MethodMetadata;
protected abstract buildMethodSignature(...): string;
protected abstract buildMethodBody(...): string;
```

### 2. Strategy Pattern

**Where:** DTO setter builders (`dto-setter-strategies/`)

**Purpose:** Select algorithm at runtime based on field type.

**Implementation:**
```typescript
export interface DtoSetterStrategy {
    canHandle(field: Property, entity: Entity): boolean;
    buildSetter(field: Property, entity: Entity, context: Context): string;
}

export class DtoSetterBuilder {
    private strategies: DtoSetterStrategy[] = [
        new AggregateFieldStrategy(),
        new EnumFieldStrategy(),
        new OverrideFieldStrategy(),
        new ExtractFieldStrategy(),
        new DefaultSetterStrategy()
    ];

    buildSetter(field: Property, entity: Entity, context: Context): string {
        for (const strategy of this.strategies) {
            if (strategy.canHandle(field, entity)) {
                return strategy.buildSetter(field, entity, context);
            }
        }
        throw new Error(`No strategy found for field ${field.name}`);
    }
}
```

**Strategies:**
- `AggregateFieldStrategy` - Handles cross-aggregate fields
- `EnumFieldStrategy` - Handles enum fields
- `OverrideFieldStrategy` - Handles field mappings
- `ExtractFieldStrategy` - Handles extract patterns
- `DefaultSetterStrategy` - Fallback for simple fields

### 3. Facade Pattern

**Where:** Feature facades (`src/cli/features/`)

**Purpose:** Simplify complex generator interactions.

**Example:**
```typescript
// Instead of:
const entityCode = await generators.entityGenerator.generateEntity(...);
await FileWriter.writeGeneratedFile(...);
const dtoCode = await generators.dtoGenerator.generateDto(...);
await FileWriter.writeGeneratedFile(...);
const factoryCode = await generators.factoryGenerator.generateFactory(...);
await FileWriter.writeGeneratedFile(...);

// Use facade:
await EntityFeature.generateCoreComponents(aggregate, path, options, generators);
```

### 4. Builder Pattern

**Where:** Context builders (`event-context-builder.ts`, `dto-method-builder.ts`)

**Purpose:** Construct complex objects step by step.

**Example:**
```typescript
export class EventContextBuilder {
    private context: any = {};

    withAggregate(aggregate: Aggregate): this {
        this.context.aggregateName = aggregate.name;
        return this;
    }

    withEvent(event: PublishedEvent): this {
        this.context.eventName = event.name;
        this.context.eventFields = event.fields;
        return this;
    }

    withOptions(options: GenerationOptions): this {
        this.context.projectName = options.projectName;
        this.context.packageName = this.buildPackageName(options);
        return this;
    }

    build(): EventContext {
        this.validate();
        return this.context;
    }

    private validate(): void {
        if (!this.context.aggregateName) throw new Error('aggregateName required');
        if (!this.context.eventName) throw new Error('eventName required');
    }
}

// Usage
const context = new EventContextBuilder()
    .withAggregate(aggregate)
    .withEvent(event)
    .withOptions(options)
    .build();
```

### 5. Registry Pattern

**Where:** `GeneratorRegistry`, DTO schema registry

**Purpose:** Central lookup of generator instances.

**Benefits:**
- Single source of truth
- Dependency injection
- Testability (can swap implementations)

## Generator Dependencies

### Dependency Graph

```
EntityGenerator
  ↓
TypeResolver
  ↓
UnifiedTypeResolver

DtoGenerator
  ↓
TypeResolver, DtoSchemaService
  ↓
UnifiedTypeResolver, DtoSchemaRegistry

ServiceGenerator
  ↓
EntityGenerator, DtoGenerator, RepositoryGenerator
  ↓
(transitively all their dependencies)

ControllerGenerator
  ↓
ServiceGenerator, DtoGenerator
  ↓
(transitively all their dependencies)
```

### Dependency Injection

```typescript
export class ServiceGenerator extends GeneratorBase {
    constructor(
        private entityGenerator?: EntityGenerator,
        private dtoGenerator?: DtoGenerator,
        options?: GeneratorOptions
    ) {
        super(options);
        this.entityGenerator = entityGenerator || new EntityGenerator();
        this.dtoGenerator = dtoGenerator || new DtoGenerator();
    }

    async generateService(aggregate: Aggregate, options: GenerationOptions): Promise<string> {
        // Use injected dependencies
        const entity = await this.entityGenerator.findRootEntity(aggregate);
        const dto = await this.dtoGenerator.generateDto(entity, options);
        // ...
    }
}
```

## Error Handling in Generators

### Contextual Errors

```typescript
export class EntityGenerator extends GeneratorBase {
    async generateEntity(entity: Entity, options: GenerationOptions): Promise<string> {
        try {
            // Generate entity
        } catch (error) {
            ErrorHandler.handle(
                error instanceof Error ? error : new Error(String(error)),
                ErrorUtils.aggregateContext(
                    'generate entity',
                    entity.name,
                    'entity-generator',
                    { hasInvariants: entity.invariants.length > 0 }
                ),
                ErrorSeverity.FATAL
            );
            throw error;
        }
    }
}
```

### Error Recovery

```typescript
await ErrorHandler.wrapAsync(
    async () => {
        const serviceCode = await generators.serviceGenerator.generateService(aggregate, options);
        await FileWriter.writeGeneratedFile(servicePath, serviceCode);
    },
    ErrorUtils.aggregateContext('generate service', aggregate.name, 'service-generator'),
    ErrorSeverity.ERROR  // Don't stop on service generation failure
);
```

## Next Steps

Explore related topics:

- **[07-Template-System](07-Template-System.md)** - Handlebars templates and rendering
- **[08-Microservices-Generators](08-Microservices-Generators.md)** - Deep dive into specific generators
- **[09-Design-Patterns](09-Design-Patterns.md)** - Comprehensive pattern guide

---

**Previous:** [05-Generation-Pipeline](05-Generation-Pipeline.md) | **Next:** [07-Template-System](07-Template-System.md)
