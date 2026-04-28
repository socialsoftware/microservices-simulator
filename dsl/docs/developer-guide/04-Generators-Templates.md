# Generators and Templates

This chapter explains how generators produce Java code from AST nodes, the template system, and how the different generator categories work.

## Generator Architecture

Each generator reads AST nodes and produces Java source code strings. Two approaches are used:

1. **Template-based** — Handlebars templates rendered with a context object. Used for structured Java files with predictable shapes (entities, DTOs, repositories).
2. **Inline string building** — direct string concatenation in TypeScript. Used for generators where the output is highly dynamic (services, workflows, commands).

## Generator Registry

`GeneratorRegistryFactory.createRegistry()` (`src/cli/engine/generator-registry.ts`) creates instances of all generators:

```typescript
const GENERATOR_FACTORIES: Record<keyof GeneratorRegistry, () => any> = {
    entityGenerator: () => new EntityGenerator(),
    dtoGenerator: () => new DtoGenerator(),
    serviceGenerator: () => new ServiceGenerator(),
    repositoryGenerator: () => new RepositoryGenerator(),
    factoryGenerator: () => new FactoryGenerator(),
    eventGenerator: () => new EventGenerator(),
    // ... 17 generators total
};
```

Generators are lazily instantiated and cached.

## Generator Categories

### Microservices Generators (`generators/microservices/`)

These produce the core aggregate code:

| Generator | Output | Template |
|-----------|--------|----------|
| `EntityGenerator` | `Entity.java` | `aggregate-base.hbs` |
| `DtoGenerator` | `EntityDto.java` | `dto-class.hbs` |
| `FactoryGenerator` | `EntityFactory.java` | `factory-interface.hbs` |
| `RepositoryGenerator` | `EntityRepository.java` | `repository-interface.hbs` |
| `ServiceGenerator` | `EntityService.java` | Inline (complex logic) |
| `CommandGenerator` | `Create/Get/Update/DeleteEntityCommand.java` | Inline |
| `CommandHandlerGenerator` | `EntityCommandHandler.java` | Inline |

**Entity builders** (`entity/builders/`): specialized classes that assemble parts of entity code — imports, constructors, invariants, event subscriptions, DTO setters.

**Service sub-generators** (`service/`):
- `crud/` — CRUD operation generators (create, read, update, delete, getAll)
- `collection/` — collection CRUD (add, remove, get, update for Set/List properties)
- `default/` — action method generator (translates DSL action bodies to Java), structure generator (imports, class declaration, dependencies)
- `extension/` — service extension hooks

### Coordination Generators (`generators/coordination/`)

These produce the orchestration layer:

| Generator | Output |
|-----------|--------|
| `FunctionalitiesGenerator` | `EntityFunctionalities.java` — orchestration wrapper |
| `ControllerGenerator` | `EntityController.java` — REST endpoints |
| `EventProcessingGenerator` | `EntityEventProcessing.java` — event routing |
| `ConfigurationGenerator` | Spring configuration files |
| `TopLevelWorkflowGenerator` | `Workflow.java`, `WorkflowRequestDto.java`, `WorkflowController.java` |

**`TopLevelWorkflowGenerator`** (`workflows/top-level-workflow-generator.ts`) is fully inline. It:
- Builds a saga-state index from all models' `SagaStatesBlock` declarations
- Chains steps sequentially via `setDependencies`
- Auto-wraps cross-aggregate projections (e.g., `new BookingUser(this.createUser)`)
- Emits `verifySagaState`/`registerSagaState` for steps with `lock` clauses

### Saga Generators (`generators/sagas/`)

These produce saga-specific variants:

| Generator | Output |
|-----------|--------|
| `SagaGenerator` | `SagaEntity.java`, `SagaEntityDto.java`, `EntitySagaState.java`, saga factory, saga repository |
| `SagaCrudGenerator` | Delegates to 5 operation generators |
| `SagaCreateGenerator` | `CreateEntityFunctionalitySagas.java` |
| `SagaReadGenerator` | `GetEntityByIdFunctionalitySagas.java` |
| `SagaReadAllGenerator` | `GetAllEntitysFunctionalitySagas.java` |
| `SagaUpdateGenerator` | `UpdateEntityFunctionalitySagas.java` |
| `SagaDeleteGenerator` | `DeleteEntityFunctionalitySagas.java` |
| `SagaFunctionalityGenerator` | Custom method saga functionalities |

The CRUD saga generators (`operations/`) extend `SagaFunctionalityGeneratorBase`, which provides shared logic for imports, fields, constructors, and class assembly. Each operation overrides `buildWorkflowMethod()` to emit the specific `SagaStep` lambda with saga-state enforcement (`verifySagaState` + `registerSagaState`).

## Template System

### Handlebars Templates

Templates live in `src/cli/templates/` and are copied to `out/cli/templates/` during build. They use [Handlebars](https://handlebarsjs.com/) syntax:

```handlebars
package {{packageName}};

{{#each imports}}
{{this}}
{{/each}}

public class {{className}} extends {{baseClass}} {
{{#each fields}}
    private {{this.type}} {{this.name}};
{{/each}}
}
```

### TemplateManager

`src/cli/utils/template-manager.ts` loads and caches templates:

```typescript
const template = this.loadTemplate('saga/saga-state.hbs');
const rendered = this.renderTemplate(template, contextObject);
```

### Context Building

Generators build a context object that maps to template placeholders:

```typescript
const context = {
    packageName: 'com.example.microservices.user.aggregate',
    className: 'User',
    imports: ['import jakarta.persistence.Entity;', ...],
    fields: [{ type: 'String', name: 'username' }, ...],
    // ...
};
```

## Key Utility: ActionMethodGenerator

`generators/microservices/service/default/action-method-generator.ts` translates DSL action bodies to Java. It handles:

- `CreateActionStatement` → DTO population + factory call + registerChanged
- `LoadActionStatement` → aggregateLoadAndRegisterRead + copy-on-write
- `AssignActionStatement` → setter call + mark aggregate dirty
- `FindActionStatement` → stream filter on collection + parent aggregate registered as changed
- Preconditions → if-guard with exception throw
- Publishes → event creation + registerEvent

This is the most complex generator. It tracks aliases (`load X as alias`), dirty state (which aggregates need `registerChanged`), and parent-child relationships (`find` aliases propagate changes to their parent aggregate).

## Key Utility: aggregate-helpers.ts

The most important utility file. It:

1. **Maintains a global model registry** for cross-file type inference
2. **Extracts entities, methods, events, repository** from aggregate AST nodes via lazy properties
3. **Synthesizes `webApiEndpoints`** from `@PostMapping` annotations on methods — translating method-level annotations into endpoint objects that controller/functionalities generators consume
4. **Synthesizes `interInvariants`** from `subscribe` blocks with `when` conditions — backwards-compatible with generators that read the old interInvariant structure
5. **Infers published events** from `publishes` clauses in method action bodies
6. **Resolves types** for cross-aggregate field mappings (including extract patterns with dotted paths)

## Shared Generator File: SagaStateConverter

`generators/microservices/shared/saga-state-generator.ts` generates `SagaStateConverter.java` — a JPA `AttributeConverter` that persists the `sagaState` interface field. Without it, Hibernate silently ignores the field. Uses `getDeclaringClass()` to handle enum bodies with method overrides.

---

**Previous:** [03-Grammar](03-Grammar.md) | **Next:** [05-Extending-The-DSL](05-Extending-The-DSL.md)
