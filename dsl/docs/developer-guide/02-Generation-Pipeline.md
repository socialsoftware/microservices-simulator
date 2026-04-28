# Generation Pipeline

This chapter traces how a `.nebula` file becomes generated Java code, step by step.

## Pipeline Overview

```
.nebula files → Parse → Validate → Generate per aggregate → Write files
```

The entire pipeline lives in `CodeGenerator.generateCode()` (`src/cli/engine/code-generator.ts`).

## Step 1: Discovery and Parsing

```typescript
const nebulaFiles = await collectNebulaFiles(inputPath);
const services = createNebulaServices(NodeFileSystem).nebulaServices;
await this.loadLanguageDocuments(services, nebulaFiles);
const models = await this.parseModels(nebulaFiles, services);
registerAllModels(models);
```

1. `collectNebulaFiles` recursively finds all `.nebula` files in the input directory
2. Langium parses each file into an AST `Model` node
3. `loadLanguageDocuments` runs validation (semantic checks from `src/language/validation/`)
4. `registerAllModels` stores all models in a global registry for cross-file type inference

## Step 2: Configuration

```typescript
const config = await this.setupConfiguration(opts, inputPath);
const paths = await ProjectSetup.setupProjectPaths(config.baseOutputDir, inputPath, config.projectName);
```

- Loads `nebula.config.json` (base package, framework version)
- Derives project name from directory name
- Creates output directory structure

## Step 3: Validation

```typescript
await this.validateModels(models, config);
```

Runs the generator-side validation system (distinct from Langium validation). Checks cross-aggregate constraints that require seeing all models together.

## Step 4: Per-Aggregate Generation

```typescript
for (const aggregate of model.aggregates) {
    initializeAggregateProperties(aggregate);
    await EntityFeature.generateCoreComponents(aggregate, path, options, generators);
    await ServiceFeature.generateService(aggregate, path, options, generators);
    await EventsFeature.generateEvents(aggregate, path, options, generators);
    await CoordinationFeature.generateCoordination(aggregate, path, options, generators, aggregates);
    await WebApiFeature.generateWebApi(aggregate, path, options, generators, aggregates);
    await SagaFeature.generateSaga(aggregate, path, options, generators, aggregates);
    // + CommandGenerator, CommandHandlerGenerator
}
```

Each feature facade calls its generators, collects the output strings, and writes them via `FileWriter`. The order matters — entities must be generated before services that reference them.

### `initializeAggregateProperties`

This function (in `aggregate-helpers.ts`) adds lazy-evaluated properties to the AST aggregate node:
- `aggregate.entities` — all Entity elements
- `aggregate.methods` — all Method elements
- `aggregate.repository` — the Repository block
- `aggregate.events` — the Events block (with synthesized inter-invariants and inferred published events)
- `aggregate.webApiEndpoints` — synthesized from `@PostMapping` annotations on methods

These properties are used throughout the generators. The synthesis step is important: `@PostMapping` annotations on methods are translated into endpoint objects that the controller and functionalities generators consume.

## Step 5: Top-Level Workflows

```typescript
const topLevelWorkflows = models.flatMap(m => m.topLevelWorkflows || []);
for (const wf of topLevelWorkflows) {
    const files = wfGenerator.generateAll(wf, aggregates, { projectName, basePackage, models });
    // writes: <Name>Workflow.java, <Name>WorkflowRequestDto.java, <Name>WorkflowController.java
}
```

Top-level `Workflow` blocks are processed after all aggregates, since they reference services from multiple aggregates.

## Step 6: Global Resources

```typescript
// Main application class
generators.integrationGenerator.generateIntegration(...)
// Exception handling
generators.exceptionGenerator.generate(...)
// Global web API (BehaviourController, TracesController)
WebApiFeature.generateGlobalWebApi(...)
// Service routing enum
serviceMappingGenerator.generate(aggregates, ...)
// Shared components (enums, saga states, saga state converter)
sharedFeature.generateSharedComponents({ models })
// Build files
TemplateGenerators.generatePomXml(projectName)
TemplateGenerators.generateGitignore()
```

## Data Flow: AST → Java

```
nebula.langium          → Langium parser → AST (Model, Aggregate, Entity, Method, ...)
                                              ↓
aggregate-helpers.ts    → initializeAggregateProperties() → enriched AST
                                              ↓
Feature facades         → call generators with AST nodes + options
                                              ↓
Generators              → read AST, build context, render templates/strings
                                              ↓
FileWriter              → write .java files to output directory
```

## GenerationOptions

Passed to every generator:

```typescript
interface GenerationOptions {
    projectName: string;
    outputPath: string;
    basePackage?: string;
    consistencyModels?: any;
    allModels?: Model[];
    dtoSchemaRegistry?: any;
}
```

The `basePackage` determines the root Java package. The `dtoSchemaRegistry` (built by `DtoSchemaService`) provides field metadata for DTO generation.

---

**Previous:** [01-Architecture](01-Architecture.md) | **Next:** [03-Grammar](03-Grammar.md)
