# Design Patterns

This chapter explores the design patterns used throughout the Nebula DSL codebase, explaining when and how to use each pattern.

## Pattern Catalog

| Pattern | Where Used | Purpose |
|---------|------------|---------|
| **Template Method** | MethodGeneratorTemplate, CrudGenerator | Define algorithm skeleton |
| **Strategy** | DTO setter builders | Select algorithm at runtime |
| **Facade** | Feature facades (EntityFeature, etc.) | Simplify complex interactions |
| **Builder** | Context builders, EventContextBuilder | Construct complex objects step by step |
| **Registry** | GeneratorRegistry, ModelRegistry | Central lookup and management |
| **Singleton** | TemplateManager, GlobalConfig | Single instance coordination |
| **Factory** | GeneratorRegistryFactory | Create related objects |
| **Visitor** | (Future) AST traversal | Separate algorithm from structure |

## Template Method Pattern

### Purpose

Define the skeleton of an algorithm in a base class, letting subclasses override specific steps without changing the algorithm's structure.

### Where Used

`MethodGeneratorTemplate` - Base class for all method generators.

### Implementation

```typescript
export abstract class MethodGeneratorTemplate extends GeneratorBase {

    // Template method - defines algorithm
    generate(aggregate: Aggregate, options: GenerationOptions): string {
        // Step 1: Extract metadata
        const metadata = this.extractMetadata(aggregate, options);

        // Step 2: Build method signature
        const signature = this.buildMethodSignature(metadata);

        // Step 3: Build method body
        const body = this.buildMethodBody(metadata);

        // Step 4: Build event handling (optional)
        const eventHandling = this.buildEventHandling(metadata);

        // Step 5: Build error handling (optional)
        const errorHandling = this.buildErrorHandling(metadata);

        // Step 6: Assemble complete method
        return this.assembleMethod(signature, body, eventHandling, errorHandling, metadata);
    }

    // Abstract methods - subclasses must implement
    protected abstract extractMetadata(
        aggregate: Aggregate,
        options: GenerationOptions
    ): MethodMetadata;

    protected abstract buildMethodSignature(metadata: MethodMetadata): string;

    protected abstract buildMethodBody(metadata: MethodMetadata): string;

    // Hook methods - subclasses can override
    protected buildEventHandling(metadata: MethodMetadata): string {
        return ''; // Default: no events
    }

    protected buildErrorHandling(metadata: MethodMetadata): string {
        // Default error handling
        return ExceptionGenerator.generateCatchBlock(...);
    }

    protected assembleMethod(...parts: any[]): string {
        // Default assembly
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
            parameters: [
                { name: 'dto', type: `Create${aggregate.name}RequestDto` },
                { name: 'unitOfWork', type: 'UnitOfWork' }
            ],
            returnType: `${aggregate.name}Dto`,
            projectName: options.projectName
        };
    }

    protected buildMethodSignature(metadata: MethodMetadata): string {
        return `public ${metadata.returnType} ${metadata.methodName}(${this.buildParameterList(metadata.parameters)})`;
    }

    protected buildMethodBody(metadata: MethodMetadata): string {
        const lower = this.lowercase(metadata.aggregateName);
        return `
            ${metadata.aggregateName} ${lower} =
                ${lower}Factory.create${metadata.aggregateName}(null, dto);
            ${lower} = unitOfWorkService.registerChanged(${lower}, unitOfWork);
            return ${lower}Factory.create${metadata.aggregateName}Dto(${lower});
        `;
    }

    protected buildEventHandling(metadata: MethodMetadata): string {
        // Create operations don't publish events (override default)
        return '';
    }
}
```

### Generated Output

```java
public UserDto createUser(CreateUserRequestDto dto, UnitOfWork unitOfWork) {
    try {
        User user = userFactory.createUser(null, dto);
        user = unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    } catch (Exception e) {
        throw new AnswersException(ERROR_CREATING_USER, e.getMessage());
    }
}
```

### When to Use

- You have an algorithm with a fixed structure but variable steps
- You want to enforce a consistent pattern across implementations
- Subclasses should only change specific parts of the algorithm

### Benefits

- Code reuse (common logic in base class)
- Consistency (all subclasses follow same structure)
- Flexibility (subclasses customize specific steps)

## Strategy Pattern

### Purpose

Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it.

### Where Used

DTO setter generation - Different strategies for different field types.

### Implementation

**Strategy Interface:**

```typescript
export interface DtoSetterStrategy {
    canHandle(field: Property, entity: Entity): boolean;
    buildSetter(field: Property, entity: Entity, context: Context): string;
}
```

**Concrete Strategies:**

```typescript
// Strategy 1: Aggregate fields (cross-aggregate references)
export class AggregateFieldStrategy implements DtoSetterStrategy {
    canHandle(field: Property, entity: Entity): boolean {
        return entity.aggregateRef !== undefined && this.isAggregateField(field);
    }

    buildSetter(field: Property, entity: Entity, context: Context): string {
        const aggregateId = `${field.name}AggregateId`;
        return `
            if (dto.get${this.capitalize(aggregateId)}() != null) {
                ${field.name}.set${this.capitalize(aggregateId)}(dto.get${this.capitalize(aggregateId)}());
            }
        `;
    }
}

// Strategy 2: Enum fields
export class EnumFieldStrategy implements DtoSetterStrategy {
    canHandle(field: Property, entity: Entity): boolean {
        return this.isEnumType(field.type);
    }

    buildSetter(field: Property, entity: Entity, context: Context): string {
        return `
            if (dto.get${this.capitalize(field.name)}() != null) {
                this.${field.name} = ${field.type}.valueOf(dto.get${this.capitalize(field.name)}());
            }
        `;
    }
}

// Strategy 3: Default (simple fields)
export class DefaultSetterStrategy implements DtoSetterStrategy {
    canHandle(field: Property, entity: Entity): boolean {
        return true; // Accepts all fields as fallback
    }

    buildSetter(field: Property, entity: Entity, context: Context): string {
        return `
            if (dto.get${this.capitalize(field.name)}() != null) {
                this.${field.name} = dto.get${this.capitalize(field.name)}();
            }
        `;
    }
}
```

**Context (Strategy Selector):**

```typescript
export class DtoSetterBuilder {
    private strategies: DtoSetterStrategy[] = [
        new AggregateFieldStrategy(),
        new EnumFieldStrategy(),
        new OverrideFieldStrategy(),
        new ExtractFieldStrategy(),
        new DefaultSetterStrategy()  // Must be last (fallback)
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

### Usage

```typescript
const builder = new DtoSetterBuilder();

for (const field of entity.properties) {
    const setter = builder.buildSetter(field, entity, context);
    setters.push(setter);
}
```

### When to Use

- You have multiple algorithms for the same task
- You want to select the algorithm at runtime
- You want to isolate algorithm implementation details

### Benefits

- Open/Closed Principle (add new strategies without modifying existing code)
- Single Responsibility (each strategy handles one algorithm)
- Runtime flexibility (select strategy based on context)

## Facade Pattern

### Purpose

Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use.

### Where Used

Feature facades - Coordinate multiple generators for a complete feature.

### Implementation

```typescript
export class EntityFeature {
    static async generateCoreComponents(
        aggregate: Aggregate,
        aggregatePath: string,
        options: GenerationOptions,
        generators: GeneratorRegistry
    ): Promise<void> {
        // Facade coordinates multiple generators

        // 1. Generate entities
        for (const entity of aggregate.entities) {
            const entityCode = await generators.entityGenerator.generateEntity(entity, options);
            const entityPath = path.join(aggregatePath, 'aggregate', `${entity.name}.java`);
            await FileWriter.writeGeneratedFile(entityPath, entityCode, `entity ${entity.name}`);
        }

        // 2. Generate DTOs
        const dtoCode = await generators.dtoGenerator.generateDto(entity, options);
        const dtoPath = path.join(aggregatePath, '..', '..', 'shared', 'dtos', `${entity.name}Dto.java`);
        await FileWriter.writeGeneratedFile(dtoPath, dtoCode, `DTO ${entity.name}Dto`);

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
    }
}
```

### Client Code

```typescript
// Instead of this (complex subsystem interaction):
const entityCode = await generators.entityGenerator.generateEntity(...);
await FileWriter.writeGeneratedFile(...);
const dtoCode = await generators.dtoGenerator.generateDto(...);
await FileWriter.writeGeneratedFile(...);
const factoryCode = await generators.factoryGenerator.generateFactory(...);
await FileWriter.writeGeneratedFile(...);
const repositoryCode = await generators.repositoryGenerator.generateRepository(...);
await FileWriter.writeGeneratedFile(...);

// Use this (simplified facade):
await EntityFeature.generateCoreComponents(aggregate, aggregatePath, options, generators);
```

### When to Use

- Complex subsystem with many classes
- You want to provide a simple interface
- You want to decouple clients from subsystem details

### Benefits

- Simplified interface (hide complexity)
- Reduced coupling (clients depend on facade, not subsystem)
- Layered architecture (facades define layers)

## Builder Pattern

### Purpose

Separate the construction of a complex object from its representation so that the same construction process can create different representations.

### Where Used

Event context builders, template context builders.

### Implementation

```typescript
export class EventContextBuilder {
    private context: Partial<EventContext> = {};

    withAggregate(aggregate: Aggregate): this {
        this.context.aggregateName = aggregate.name;
        this.context.aggregateLowerCase = aggregate.name.toLowerCase();
        return this;
    }

    withEvent(event: PublishedEvent): this {
        this.context.eventName = event.name;
        this.context.eventFields = event.fields.map(f => ({
            name: f.name,
            type: this.typeResolver.resolveJavaType(f.type)
        }));
        return this;
    }

    withOptions(options: GenerationOptions): this {
        this.context.projectName = options.projectName;
        this.context.packageName = this.buildPackageName(options);
        return this;
    }

    withImports(imports: string[]): this {
        this.context.imports = imports;
        return this;
    }

    build(): EventContext {
        this.validate();
        return this.context as EventContext;
    }

    private validate(): void {
        if (!this.context.aggregateName) {
            throw new Error('aggregateName is required');
        }
        if (!this.context.eventName) {
            throw new Error('eventName is required');
        }
        if (!this.context.projectName) {
            throw new Error('projectName is required');
        }
    }

    private buildPackageName(options: GenerationOptions): string {
        return `${options.basePackage}.${options.projectName}.microservices.${this.context.aggregateLowerCase}.events.publish`;
    }
}
```

### Usage

```typescript
const context = new EventContextBuilder()
    .withAggregate(aggregate)
    .withEvent(event)
    .withOptions(options)
    .withImports(['java.util.*', 'javax.persistence.*'])
    .build();

const code = templateManager.renderTemplate('events/published-event.hbs', context);
```

### When to Use

- Object construction is complex with many parameters
- You want a fluent API for construction
- Construction process should be independent of parts

### Benefits

- Fluent interface (readable construction)
- Immutable construction (build() finalizes)
- Validation (ensure required fields present)

## Registry Pattern

### Purpose

Provide a well-known object that other objects can use to find common objects and services.

### Where Used

- GeneratorRegistry - Manage generator instances
- ModelRegistry - Cross-file aggregate lookup
- DtoSchemaRegistry - DTO schema lookup

### Implementation

**Generator Registry:**

```typescript
export interface GeneratorRegistry {
    entityGenerator: EntityOrchestrator;
    dtoGenerator: DtoGenerator;
    serviceGenerator: ServiceGenerator;
    repositoryGenerator: RepositoryGenerator;
    factoryGenerator: FactoryGenerator;
    eventGenerator: EventGenerator;
    controllerGenerator: ControllerGenerator;
    sagaGenerator: SagaGenerator;
    validationSystem: ValidationSystem;
}

export class GeneratorRegistryFactory {
    static createRegistry(): GeneratorRegistry {
        return {
            entityGenerator: new EntityOrchestrator(),
            dtoGenerator: new DtoGenerator(),
            serviceGenerator: new ServiceGenerator(),
            repositoryGenerator: new RepositoryGenerator(),
            factoryGenerator: new FactoryGenerator(),
            eventGenerator: new EventGenerator(),
            controllerGenerator: new ControllerGenerator(),
            sagaGenerator: new SagaGenerator(),
            validationSystem: new ValidationSystem()
        };
    }
}
```

**Model Registry:**

```typescript
let globalModelRegistry: Model[] = [];

export function registerAllModels(models: Model[]) {
    globalModelRegistry = models;
}

export function findAggregateInModels(
    aggregateName: string,
    models?: Model[]
): Aggregate | undefined {
    const searchModels = models || globalModelRegistry;
    for (const model of searchModels) {
        const aggregate = model.aggregates.find(a => a.name === aggregateName);
        if (aggregate) return aggregate;
    }
    return undefined;
}
```

### Usage

```typescript
// Register models once
registerAllModels(models);

// Use anywhere
const courseAggregate = findAggregateInModels('Course');
if (courseAggregate) {
    const courseEntity = findRootEntity(courseAggregate);
    // Use course entity for type inference
}
```

### When to Use

- You need global access to a set of objects
- Object lookup should be centralized
- You want to avoid passing objects through many layers

### Benefits

- Single source of truth
- Centralized management
- Global accessibility

## Singleton Pattern

### Purpose

Ensure a class has only one instance and provide a global point of access to it.

### Where Used

TemplateManager, GlobalConfig.

### Implementation

```typescript
export class TemplateManager {
    private static instance: TemplateManager;
    private templateCache = new Map<string, TemplateCacheEntry>();
    private config: TemplateManagerConfig;

    // Private constructor prevents direct instantiation
    private constructor(config: TemplateManagerConfig) {
        this.config = config;
        this.registerHandlebarsHelpers();
        this.registerPartials();
    }

    // Global access point
    static getInstance(config?: TemplateManagerConfig): TemplateManager {
        if (!TemplateManager.instance) {
            TemplateManager.instance = new TemplateManager(
                config || { enableCache: true, enableDevMode: false }
            );
        }
        return TemplateManager.instance;
    }

    // Instance methods
    renderTemplate(templatePath: string, context: any): string {
        // ...
    }

    clearCache(): void {
        this.templateCache.clear();
    }
}
```

### Usage

```typescript
// Always returns same instance
const manager1 = TemplateManager.getInstance();
const manager2 = TemplateManager.getInstance();

console.log(manager1 === manager2); // true
```

### When to Use

- Exactly one instance needed (resource manager, cache)
- Global access required
- Lazy initialization desired

### Benefits

- Controlled access to sole instance
- Reduced namespace pollution
- Lazy initialization possible

### Drawbacks

- Harder to test (global state)
- Can hide dependencies
- Not suitable for multi-threading (Node.js is single-threaded, so OK here)

## Putting It All Together

### Example: CRUD Method Generation

Shows multiple patterns working together:

```typescript
// 1. Registry Pattern - Get generators
const generators = GeneratorRegistryFactory.createRegistry();

// 2. Facade Pattern - Coordinate generation
await ServiceFeature.generateService(aggregate, aggregatePath, options, generators);

// Inside ServiceFeature.generateService():
class ServiceFeature {
    static async generateService(...) {
        // 3. Template Method Pattern - Generate each CRUD method
        const createMethod = new CrudCreateGenerator().generate(aggregate, options);
        const readMethod = new CrudReadGenerator().generate(aggregate, options);
        const updateMethod = new CrudUpdateGenerator().generate(aggregate, options);
        const deleteMethod = new CrudDeleteGenerator().generate(aggregate, options);

        // 4. Builder Pattern - Build service context
        const context = new ServiceContextBuilder()
            .withAggregate(aggregate)
            .withMethods([createMethod, readMethod, updateMethod, deleteMethod])
            .withOptions(options)
            .build();

        // 5. Singleton Pattern - Render template
        const templateManager = TemplateManager.getInstance();
        const code = templateManager.renderTemplate('service/service.hbs', context);

        return code;
    }
}

// Inside CrudCreateGenerator (Template Method subclass):
class CrudCreateGenerator extends MethodGeneratorTemplate {
    // 6. Strategy Pattern - Build DTO setters
    protected buildMethodBody(...) {
        const setterBuilder = new DtoSetterBuilder();
        const setters = entity.properties.map(prop =>
            setterBuilder.buildSetter(prop, entity, context)
        );
        return this.assembleBody(setters);
    }
}
```

## Pattern Selection Guide

| Use Case | Pattern |
|----------|---------|
| Fixed algorithm structure, variable steps | **Template Method** |
| Multiple algorithms for same task | **Strategy** |
| Complex subsystem with many classes | **Facade** |
| Complex object construction | **Builder** |
| Global object lookup | **Registry** |
| Single instance needed | **Singleton** |
| Create related objects | **Factory** |

## Next Steps

- **[10-Adding-DSL-Features](10-Adding-DSL-Features.md)** - Apply patterns to new features
- **[06-Generator-System](06-Generator-System.md)** - Generator architecture
- **[08-Microservices-Generators](08-Microservices-Generators.md)** - Real-world pattern usage

---

**Previous:** [08-Microservices-Generators](08-Microservices-Generators.md) | **Next:** [10-Adding-DSL-Features](10-Adding-DSL-Features.md)
