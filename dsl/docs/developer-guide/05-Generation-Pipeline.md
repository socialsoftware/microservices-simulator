# Generation Pipeline

This chapter explains how Nebula transforms `.nebula` files into production-ready Java code through a multi-stage pipeline.

## High-Level Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    1. DISCOVERY PHASE                       │
│  • Scan directory for .nebula files                         │
│  • Collect file paths                                       │
│  • Validate file existence                                  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    2. PARSING PHASE                         │
│  • Load files into Langium workspace                        │
│  • Parse grammar → AST construction                         │
│  • Build cross-references                                   │
│  • Register all models in global registry                   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    3. VALIDATION PHASE                      │
│  • Semantic validation (validators)                         │
│  • Check referential integrity                              │
│  • Validate invariant expressions                           │
│  • Report errors or proceed                                 │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    4. CONFIGURATION PHASE                   │
│  • Setup output directories                                 │
│  • Load configuration (nebula.config.json)                  │
│  • Initialize generator registry                            │
│  • Build DTO schema registry                                │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    5. GENERATION PHASE                      │
│  • Feature facades orchestrate generation                   │
│  • Generators produce code from AST                         │
│  • Templates render with context                            │
│  • Files written to output directory                        │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    6. COMPLETION PHASE                      │
│  • Write project files (pom.xml, .gitignore)                │
│  • Generate configuration files                             │
│  • Print statistics and summary                             │
└─────────────────────────────────────────────────────────────┘
```

## Phase 1: Discovery

**File:** `src/cli/utils/file-utils.ts`

### Process

```typescript
export async function collectNebulaFiles(inputPath: string): Promise<string[]> {
    const stats = await fs.stat(inputPath);

    if (stats.isFile()) {
        // Single file
        if (inputPath.endsWith('.nebula')) {
            return [inputPath];
        }
        throw new Error(`Not a .nebula file: ${inputPath}`);
    }

    // Directory - recursively find all .nebula files
    const files: string[] = [];

    async function scan(dir: string) {
        const entries = await fs.readdir(dir, { withFileTypes: true });

        for (const entry of entries) {
            const fullPath = path.join(dir, entry.name);

            if (entry.isDirectory()) {
                await scan(fullPath);
            } else if (entry.name.endsWith('.nebula')) {
                files.push(fullPath);
            }
        }
    }

    await scan(inputPath);
    return files;
}
```

### Output

```
Found 9 Nebula files:
  • /path/to/abstractions/answers/user.nebula
  • /path/to/abstractions/answers/course.nebula
  • /path/to/abstractions/answers/execution.nebula
  • ...
```

## Phase 2: Parsing

**File:** `src/cli/engine/code-generator.ts` (lines 225-274)

### Process

```typescript
private static async loadLanguageDocuments(
    services: any,
    nebulaFiles: string[]
): Promise<void> {
    // 1. Load documents into Langium workspace
    for (const filePath of nebulaFiles) {
        await services.shared.workspace.LangiumDocuments.getOrCreateDocument(
            URI.file(filePath)
        );
    }

    // 2. Build AST and resolve cross-references
    await services.shared.workspace.DocumentBuilder.build(
        Array.from(services.shared.workspace.LangiumDocuments.all),
        { validation: true }
    );

    // 3. Initialize aggregate properties
    const allDocuments = Array.from(services.shared.workspace.LangiumDocuments.all);
    for (const document of allDocuments) {
        const model = document.parseResult?.value as Model;
        if (model?.aggregates) {
            for (const aggregate of model.aggregates) {
                initializeAggregateProperties(aggregate);
            }
        }
    }

    // 4. Check for validation errors
    for (const document of allDocuments) {
        const errors = document.diagnostics?.filter(e => e.severity === 1);
        if (errors && errors.length > 0) {
            // Report errors and exit
        }
    }
}
```

### AST Construction

**Input DSL:**
```nebula
Aggregate User {
    Root Entity User {
        String name
        UserRole role
    }
}
```

**Generated AST (TypeScript):**
```typescript
{
    $type: 'Model',
    aggregates: [
        {
            $type: 'Aggregate',
            name: 'User',
            annotations: [],
            generateCrud: false,
            entities: [
                {
                    $type: 'Entity',
                    name: 'User',
                    isRoot: true,
                    properties: [
                        {
                            $type: 'Property',
                            name: 'name',
                            type: {
                                $type: 'PrimitiveType',
                                typeName: 'String'
                            }
                        },
                        {
                            $type: 'Property',
                            name: 'role',
                            type: {
                                $type: 'EntityType',
                                type: { ref: UserRoleEnum }
                            }
                        }
                    ]
                }
            ]
        }
    ]
}
```

### Model Registry

After parsing, all models are registered globally:

```typescript
// In code-generator.ts (line 53)
registerAllModels(models);
```

**Purpose:**
- Enables cross-file type inference
- Allows generators to find referenced aggregates
- Supports validation across files

**Implementation (`aggregate-helpers.ts`):**
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

## Phase 3: Validation

**File:** `src/cli/engine/code-generator.ts` (lines 311-333)

### Validation System

```typescript
private static async validateModels(
    allModels: Model[],
    config: any
): Promise<void> {
    const generators = GeneratorRegistryFactory.createRegistry();

    // Collect all aggregates
    const allAggregates: Aggregate[] = [];
    for (const model of allModels) {
        allAggregates.push(...model.aggregates);
    }

    // Run validation
    const validationResult = await generators.validationSystem.validateAggregates(
        allAggregates,
        { projectName: config.projectName }
    );

    // Check result
    if (!validationResult.isValid) {
        console.error("Validation failed:");
        console.error(generators.validationSystem.getValidationReport(validationResult));
        process.exit(1);
    }

    console.log("✅ Validation passed!");
}
```

### Validation Checks

1. **Structural validation:**
   - Each aggregate has exactly one root entity
   - Only one Repository, Events, References block per aggregate
   - Required fields present

2. **Semantic validation:**
   - Cross-references point to valid aggregates
   - Event subscriptions reference existing events
   - Invariant expressions are well-formed

3. **Type validation:**
   - Property types are valid
   - Collection element types are valid
   - Cross-aggregate type inference succeeds

**Example validation error:**
```
execution.nebula:4:5 - Aggregate 'Course' not found [Course]
execution.nebula:10:9 - Property type must be a valid type [InvalidType]
```

## Phase 4: Configuration

### Output Directory Setup

**File:** `src/cli/engine/project-setup.ts`

```typescript
export class ProjectSetup {
    static async setupProjectPaths(
        baseOutputDir: string,
        inputPath: string,
        projectName: string
    ): Promise<ProjectPaths> {
        const projectPath = path.join(baseOutputDir, projectName);
        const srcPath = path.join(projectPath, 'src/main');
        const javaPath = path.join(srcPath, 'java/pt/ulisboa/tecnico/socialsoftware', projectName);
        const resourcesPath = path.join(srcPath, 'resources');

        // Create directories
        await fs.mkdir(javaPath, { recursive: true });
        await fs.mkdir(resourcesPath, { recursive: true });

        return { projectPath, srcPath, javaPath, resourcesPath };
    }
}
```

**Generated structure:**
```
applications/answers/
├── src/main/
│   ├── java/pt/ulisboa/tecnico/socialsoftware/answers/
│   └── resources/
└── pom.xml
```

### Configuration Loading

**File:** `src/cli/utils/config-loader.ts`

```typescript
export class ConfigLoader {
    static async loadConfig(
        inputPath: string,
        defaults: Partial<GenerationConfig>
    ): Promise<GenerationConfig> {
        const configPath = path.join(inputPath, 'nebula.config.json');

        if (await fs.access(configPath).then(() => true).catch(() => false)) {
            const configContent = await fs.readFile(configPath, 'utf-8');
            const config = JSON.parse(configContent);
            return { ...defaults, ...config };
        }

        return defaults as GenerationConfig;
    }
}
```

**Example `nebula.config.json`:**
```json
{
  "projectName": "answers",
  "basePackage": "pt.ulisboa.tecnico.socialsoftware",
  "consistencyModels": ["sagas", "tcc"]
}
```

### Generator Registry Initialization

```typescript
const generators = GeneratorRegistryFactory.createRegistry();
```

**Creates instances of all generators:**
- EntityGenerator
- DtoGenerator
- ServiceGenerator
- RepositoryGenerator
- FactoryGenerator
- EventGenerator
- ControllerGenerator
- SagaGenerator
- ConfigurationGenerator
- ValidationSystem

### DTO Schema Registry

```typescript
const dtoSchemaService = new DtoSchemaService();
const dtoSchemaRegistry = dtoSchemaService.buildFromModels(models);
```

**Purpose:**
- Tracks all DTO field definitions across aggregates
- Enables cross-aggregate DTO references
- Supports DTO composition and inheritance

## Phase 5: Generation

**File:** `src/cli/engine/code-generator.ts` (lines 100-128)

### Generation Loop

```typescript
for (const model of models) {
    for (const aggregate of model.aggregates) {
        console.log(`\nGenerating ${aggregate.name} components:`);

        const aggregatePath = paths.javaPath + '/microservices/' + aggregate.name.toLowerCase();

        // 1. Entity layer
        await EntityFeature.generateCoreComponents(aggregate, aggregatePath, options, generators);

        // 2. Service layer
        await ServiceFeature.generateService(aggregate, aggregatePath, options, generators);

        // 3. Event layer
        await EventsFeature.generateEvents(aggregate, aggregatePath, options, generators);

        // 4. Coordination layer
        await CoordinationFeature.generateCoordination(aggregate, paths, options, generators);

        // 5. Web API layer
        await WebApiFeature.generateWebApi(aggregate, paths, options, generators);

        // 6. Validation layer
        await ValidationFeature.generateValidation(aggregate, paths, options, generators);

        // 7. Saga layer
        await SagaFeature.generateSaga(aggregate, paths, options, generators, aggregates);
    }
}
```

### Feature Orchestration

**Example: EntityFeature**

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
    }
}
```

### Generator Invocation

**Individual generator workflow:**

```typescript
class EntityGenerator {
    async generateEntity(entity: Entity, options: GenerationOptions): Promise<string> {
        // 1. Extract metadata
        const metadata = this.extractEntityMetadata(entity, options);

        // 2. Build context for template
        const context = {
            packageName: this.buildPackageName(options.projectName, 'aggregate'),
            className: entity.name,
            imports: this.buildImports(entity),
            fields: this.buildFields(entity),
            constructors: this.buildConstructors(entity),
            methods: this.buildMethods(entity),
            invariants: this.buildInvariants(entity)
        };

        // 3. Render template
        return this.templateManager.renderTemplate('entity/entity.hbs', context);
    }
}
```

### Template Rendering

**Template:** `entity/entity.hbs`

```handlebars
package {{packageName}};

{{#each imports}}
import {{this}};
{{/each}}

@Entity
public class {{className}} extends Aggregate {
    {{#each fields}}
    private {{type}} {{name}};
    {{/each}}

    {{#each constructors}}
    {{this}}
    {{/each}}

    {{#each methods}}
    {{this}}
    {{/each}}

    {{#if invariants}}
    @Override
    public void verifyInvariants() {
        {{invariants}}
    }
    {{/if}}
}
```

**Rendered output:**
```java
package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import javax.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Entity
public class User extends Aggregate {
    private String name;
    private String username;
    private UserRole role;

    public User() {}

    public User(Integer aggregateId, UserDto dto) {
        setAggregateId(aggregateId);
        setName(dto.getName());
        setUsername(dto.getUsername());
        setRole(dto.getRole());
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public void verifyInvariants() {
        if (!(invariantNameNotBlank() && invariantUsernameNotBlank())) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
    }
}
```

## Phase 6: Completion

### Project Files

```typescript
// pom.xml
const pomContent = TemplateGenerators.generatePomXml(config.projectName);
await fs.writeFile(path.join(paths.projectPath, "pom.xml"), pomContent, 'utf-8');

// .gitignore
const gitignoreContent = TemplateGenerators.generateGitignore();
await fs.writeFile(path.join(paths.projectPath, ".gitignore"), gitignoreContent, 'utf-8');

// application.yml
await generators.configurationGenerator.generate({
    projectName: config.projectName,
    basePackage: config.basePackage,
    outputDirectory: paths.projectPath
}, { projectName: config.projectName });
```

### Statistics

```
╔══════════════════════════════════════════════════════════════════╗
║                    Generation Complete                           ║
╚══════════════════════════════════════════════════════════════════╝

📊 Summary:
  • Total Aggregates: 9
  • Total Files Generated: 187
  • Output Directory: /path/to/applications/answers
  • Generation Time: 18.4s
```

## Error Handling

### Error Context

**File:** `src/cli/utils/error-handler.ts`

```typescript
export class ErrorHandler {
    static handle(
        error: Error,
        context: ErrorContext,
        severity: ErrorSeverity,
        throwError = true
    ): void {
        console.error(`[${severity}] ${context.operation} failed`);
        console.error(`  Aggregate: ${context.aggregateName}`);
        console.error(`  Generator: ${context.generatorName}`);
        console.error(`  Error: ${error.message}`);

        if (throwError && severity === ErrorSeverity.FATAL) {
            throw error;
        }
    }
}
```

### Error Severities

- **FATAL** - Stop generation immediately
- **ERROR** - Report error but continue
- **WARNING** - Log warning and continue
- **INFO** - Informational message

**Example:**
```
[FATAL] generate entity failed
  Aggregate: User
  Generator: entity-generator
  Error: Property type 'InvalidType' not recognized
```

## Generation Performance

### Optimization Strategies

1. **Template caching:**
   ```typescript
   // Templates compiled once and cached
   private templateCache = new Map<string, HandlebarsTemplateDelegate>();
   ```

2. **Parallel generation:**
   ```typescript
   // Independent generators run in parallel
   await Promise.all([
       EntityFeature.generateCoreComponents(...),
       EventsFeature.generateEvents(...)
   ]);
   ```

3. **Incremental writes:**
   ```typescript
   // Write files as soon as generated (don't accumulate in memory)
   await FileWriter.writeGeneratedFile(entityPath, entityCode);
   ```

### Benchmarks

**Answers project (9 aggregates):**
- Discovery: 0.1s
- Parsing: 2.3s
- Validation: 0.8s
- Generation: 15.2s
- **Total: 18.4s**

**File counts:**
- Entities: 27
- DTOs: 30
- Services: 9
- Controllers: 9
- Events: 45
- Sagas: 27
- Config: 5
- **Total: 187 files**

## Debugging the Pipeline

### Enable Verbose Logging

```bash
DEBUG=nebula:* ./bin/cli.js generate ../abstractions/answers/
```

### Common Issues

1. **Parsing fails:**
   - Check grammar syntax
   - Run `npm run langium:generate` first
   - Validate `.nebula` syntax

2. **Type inference fails:**
   - Ensure referenced aggregate exists
   - Check model registry initialization
   - Verify cross-file references

3. **Template rendering fails:**
   - Check template syntax
   - Verify context data structure
   - Ensure templates copied to `out/`

4. **File write fails:**
   - Check directory permissions
   - Verify output path exists
   - Ensure no file locks

## Next Steps

Deep dive into specific components:

- **[06-Generator-System](06-Generator-System.md)** - Generator architecture and patterns
- **[07-Template-System](07-Template-System.md)** - Handlebars template system
- **[08-Microservices-Generators](08-Microservices-Generators.md)** - Entity, Service, Repository generators

---

**Previous:** [04-DSL-Features](04-DSL-Features.md) | **Next:** [06-Generator-System](06-Generator-System.md)
