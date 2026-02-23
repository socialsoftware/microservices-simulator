# Template System

This chapter covers Nebula's template-based code generation using Handlebars, including template organization, partials, helpers, context building, and best practices.

## Why Handlebars?

**Handlebars** is a logic-less template engine that separates presentation (templates) from logic (generators).

### Benefits

1. **Separation of concerns** - Templates focus on structure, generators focus on logic
2. **Readability** - Templates look like the output they generate
3. **Maintainability** - Easy to modify generated code structure
4. **Reusability** - Partials and helpers promote DRY
5. **Type safety** - Context building ensures correct data structure

### Alternatives Considered

| Template Engine | Why Not Used |
|----------------|--------------|
| **EJS** | Too much embedded JavaScript, harder to maintain |
| **Pug** | Indentation-based, not suitable for Java code |
| **Mustache** | Too limited, no helpers or partials |
| **Direct string concatenation** | Error-prone, unmaintainable |

## Template Manager

### Overview

`TemplateManager` is a singleton that handles template loading, caching, and rendering.

**File:** `src/cli/utils/template-manager.ts`

### Core Features

```typescript
export class TemplateManager {
    private static instance: TemplateManager;
    private templateCache = new Map<string, TemplateCacheEntry>();
    private templateRoot: string;
    private config: TemplateManagerConfig;

    // Singleton instance
    static getInstance(config?: TemplateManagerConfig): TemplateManager;

    // Load and compile template
    loadTemplate(templatePath: string): HandlebarsTemplateDelegate<any>;

    // Load raw template content
    loadRawTemplate(templatePath: string): string;

    // Render template with context
    renderTemplate(templatePath: string, context: any): string;

    // Preload multiple templates
    async preloadTemplates(templatePaths: string[]): Promise<void>;

    // Cache management
    clearCache(): void;
    invalidateTemplate(templatePath: string): void;
    getCacheStats(): { size: number; hitRate: number; templates: string[] };

    // Development mode - reload templates on file change
    setDevMode(enabled: boolean): void;
}
```

### Usage

```typescript
// In generators
protected renderTemplate(templatePath: string, context: any): string {
    return this.templateManager.renderTemplate(templatePath, context);
}

// Example
const code = this.renderTemplate('entity/factory-interface.hbs', {
    packageName: 'pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate',
    aggregateName: 'User',
    createMethodParams: 'Integer aggregateId, UserDto dto',
    dtoName: 'UserDto',
    lowerAggregateName: 'user'
});
```

### Template Caching

**How it works:**

```typescript
private loadAndCacheTemplate(normalizedPath: string): HandlebarsTemplateDelegate<any> {
    const fullPath = path.join(this.templateRoot, normalizedPath);

    // Read and compile template
    const rawContent = fs.readFileSync(fullPath, 'utf-8');
    const compiledTemplate = Handlebars.compile(rawContent, { noEscape: true });

    // Cache entry
    const cacheEntry: TemplateCacheEntry = {
        rawContent,
        compiledTemplate,
        lastModified: fs.statSync(fullPath).mtime,
        filePath: fullPath
    };

    this.templateCache.set(normalizedPath, cacheEntry);
    return compiledTemplate;
}
```

**Benefits:**
- Templates compiled once
- Subsequent renders are fast (no parsing)
- Dev mode can reload on file change

## Template Organization

### Directory Structure

```
src/cli/templates/
├── entity/
│   ├── factory-interface.hbs       # Factory interface
│   └── aggregate-base.hbs          # Aggregate wrapper class
├── repository/
│   └── repository-interface.hbs    # Spring Data repository
├── service/
│   ├── service-definition.hbs      # Service interface
│   └── service-implementation.hbs  # Service implementation
├── web/
│   ├── controller.hbs              # REST controller
│   ├── request-dtos.hbs            # Request DTOs
│   └── response-dtos.hbs           # Response DTOs
├── events/
│   ├── published-event.hbs         # Event class
│   ├── event-subscription.hbs      # Event subscription
│   ├── event-handler.hbs           # Event handler
│   └── event-handling.hbs          # Event handling router
├── saga/
│   ├── saga-functionality.hbs      # Saga workflows
│   ├── saga-coordination.hbs       # Saga coordination
│   └── saga-aggregate.hbs          # Saga aggregate extension
├── coordination/
│   └── functionalities.hbs         # Functionalities class
├── config/
│   ├── pom.hbs                     # Maven POM
│   ├── application.hbs             # application.yml
│   └── configuration.hbs           # Spring configuration
└── _partials/                      # Reusable fragments
    └── dto-class.hbs               # DTO structure partial
```

### Naming Conventions

- **Template files:** `kebab-case.hbs` (e.g., `factory-interface.hbs`)
- **Partials:** `kebab-case.hbs` in `_partials/` directory
- **Variables:** `camelCase` (e.g., `aggregateName`, `packageName`)

## Template Syntax

### Basic Interpolation

```handlebars
package {{packageName}};

public class {{className}} {
    private {{type}} {{fieldName}};
}
```

**Context:**
```typescript
{
    packageName: 'pt.ulisboa.tecnico.socialsoftware.answers.microservices.user',
    className: 'User',
    type: 'String',
    fieldName: 'name'
}
```

**Output:**
```java
package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user;

public class User {
    private String name;
}
```

### Conditionals

```handlebars
{{#if isRoot}}
@Entity
public class {{className}} extends Aggregate {
{{else}}
@Embeddable
public class {{className}} {
{{/if}}
    // Fields
}
```

**Context:**
```typescript
{
    isRoot: true,
    className: 'User'
}
```

**Output:**
```java
@Entity
public class User extends Aggregate {
    // Fields
}
```

### Loops

```handlebars
{{#each fields}}
private {{type}} {{name}};
{{/each}}
```

**Context:**
```typescript
{
    fields: [
        { type: 'String', name: 'name' },
        { type: 'UserRole', name: 'role' },
        { type: 'Boolean', name: 'active' }
    ]
}
```

**Output:**
```java
private String name;
private UserRole role;
private Boolean active;
```

### Nested Loops

```handlebars
{{#each methods}}
public {{returnType}} {{name}}(
    {{#each parameters}}
    {{type}} {{name}}{{#unless @last}},{{/unless}}
    {{/each}}
) {
    // Implementation
}
{{/each}}
```

**Context:**
```typescript
{
    methods: [
        {
            returnType: 'UserDto',
            name: 'createUser',
            parameters: [
                { type: 'CreateUserRequestDto', name: 'dto' },
                { type: 'UnitOfWork', name: 'unitOfWork' }
            ]
        }
    ]
}
```

**Output:**
```java
public UserDto createUser(
    CreateUserRequestDto dto,
    UnitOfWork unitOfWork
) {
    // Implementation
}
```

## Handlebars Helpers

### Built-in Helpers

Nebula registers custom helpers in `TemplateManager`:

```typescript
private registerHandlebarsHelpers(): void {
    // Comparison
    Handlebars.registerHelper('eq', (a, b) => a === b);
    Handlebars.registerHelper('ne', (a, b) => a !== b);
    Handlebars.registerHelper('lt', (a, b) => a < b);
    Handlebars.registerHelper('gt', (a, b) => a > b);
    Handlebars.registerHelper('lte', (a, b) => a <= b);
    Handlebars.registerHelper('gte', (a, b) => a >= b);

    // Boolean logic
    Handlebars.registerHelper('and', (a, b) => !!(a && b));
    Handlebars.registerHelper('or', (a, b) => !!(a || b));
    Handlebars.registerHelper('not', (a) => !a);

    // String manipulation
    Handlebars.registerHelper('capitalize', (str) =>
        str ? str.charAt(0).toUpperCase() + str.slice(1) : ''
    );
    Handlebars.registerHelper('lowercase', (str) => str ? str.toLowerCase() : '');
    Handlebars.registerHelper('uppercase', (str) => str ? str.toUpperCase() : '');

    // Utilities
    Handlebars.registerHelper('json', (context) => JSON.stringify(context));
    Handlebars.registerHelper('length', (array) => Array.isArray(array) ? array.length : 0);
}
```

### Helper Usage Examples

**Comparison:**
```handlebars
{{#if (eq type "String")}}
private String {{name}};
{{else if (eq type "Integer")}}
private Integer {{name}};
{{/if}}
```

**Boolean logic:**
```handlebars
{{#if (and isRoot hasInvariants)}}
@Override
public void verifyInvariants() {
    // Invariant checks
}
{{/if}}
```

**String manipulation:**
```handlebars
public {{capitalize returnType}} get{{capitalize fieldName}}() {
    return {{fieldName}};
}
```

## Partials

### What are Partials?

Partials are reusable template fragments that can be included in other templates.

### Defining Partials

**File:** `src/cli/templates/_partials/dto-class.hbs`

```handlebars
{{!--
  Shared DTO Class Template

  Parameters:
  - name: Class name (e.g., "CreateUserRequestDto")
  - fields: Array of field objects {name, type, required}
  - includeNotNull: Boolean - whether to add @NotNull annotations
--}}
public class {{name}} {
    {{#each fields}}
    {{#if ../includeNotNull}}{{#if required}}@NotNull{{/if}}{{/if}}
    private {{type}} {{name}};
    {{/each}}

    public {{name}}() {}

    {{#each fields}}
    public {{type}} get{{capitalize name}}() {
        return {{name}};
    }

    public void set{{capitalize name}}({{type}} {{name}}) {
        this.{{name}} = {{name}};
    }
    {{/each}}
}
```

### Using Partials

```handlebars
package {{packageName}};

import javax.validation.constraints.NotNull;

{{> dto-class name="CreateUserRequestDto" fields=requestFields includeNotNull=true}}

{{> dto-class name="UserDto" fields=dtoFields includeNotNull=false}}
```

**Context:**
```typescript
{
    packageName: 'pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos',
    requestFields: [
        { name: 'name', type: 'String', required: true },
        { name: 'role', type: 'UserRole', required: true }
    ],
    dtoFields: [
        { name: 'aggregateId', type: 'Integer', required: false },
        { name: 'name', type: 'String', required: false },
        { name: 'role', type: 'UserRole', required: false }
    ]
}
```

### Benefits of Partials

1. **DRY** - Write once, use many times
2. **Consistency** - Same structure across all DTOs
3. **Maintainability** - Update in one place
4. **Readability** - Hide complexity in reusable units

## Context Building

### Context Structure

Templates need structured data (context) to render correctly. Generators build this context.

**Example: Factory Interface Generator**

```typescript
export class FactoryGenerator extends GeneratorBase {
    async generateFactory(aggregate: Aggregate, options: GenerationOptions): Promise<string> {
        // 1. Extract data
        const rootEntity = this.findRootEntity(aggregate);
        const aggregateName = aggregate.name;
        const dtoName = `${aggregateName}Dto`;

        // 2. Build context
        const context = {
            packageName: this.generatePackageName(
                options.projectName,
                aggregateName.toLowerCase(),
                'aggregate'
            ),
            imports: this.buildImports(aggregate, options),
            aggregateName: aggregateName,
            createMethodParams: this.buildCreateMethodParams(aggregate),
            dtoName: dtoName,
            lowerAggregateName: this.toCamelCase(aggregateName)
        };

        // 3. Render template
        return this.renderTemplate('entity/factory-interface.hbs', context);
    }

    private buildCreateMethodParams(aggregate: Aggregate): string {
        return 'Integer aggregateId, ' + aggregate.name + 'Dto dto';
    }

    private buildImports(aggregate: Aggregate, options: GenerationOptions): string {
        const imports = [
            `pt.ulisboa.tecnico.socialsoftware.${options.projectName}.shared.dtos.${aggregate.name}Dto`
        ];
        return imports.map(imp => `import ${imp};`).join('\n');
    }
}
```

### Context Best Practices

1. **Flat structure** - Avoid deep nesting where possible
2. **Meaningful names** - `aggregateName` not `an`
3. **Pre-computed values** - Do logic in generator, not template
4. **Type safety** - Use TypeScript interfaces for context

**Example interface:**
```typescript
interface FactoryContext {
    packageName: string;
    imports: string;
    aggregateName: string;
    createMethodParams: string;
    dtoName: string;
    lowerAggregateName: string;
}
```

## Real-World Examples

### Example 1: Factory Interface Template

**Template:** `entity/factory-interface.hbs`

```handlebars
package {{packageName}};

{{#if imports}}
{{imports}}

{{/if}}
public interface {{aggregateName}}Factory {
    {{aggregateName}} create{{aggregateName}}({{createMethodParams}});
    {{aggregateName}} create{{aggregateName}}FromExisting({{aggregateName}} existing{{aggregateName}});
    {{dtoName}} create{{aggregateName}}Dto({{aggregateName}} {{lowerAggregateName}});
}
```

**Generated Java:**
```java
package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto dto);
    User createUserFromExisting(User existingUser);
    UserDto createUserDto(User user);
}
```

### Example 2: Repository Interface Template

**Template:** `repository/repository-interface.hbs`

```handlebars
package {{packageName}};

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import {{entityPackage}}.{{entityName}};
import java.util.*;

@Repository
public interface {{repositoryName}} extends JpaRepository<{{entityName}}, Integer> {
    {{#each customMethods}}
    {{#if query}}
    @Query("{{query}}")
    {{/if}}
    {{returnType}} {{name}}({{#each parameters}}{{#if @index}}, {{/if}}{{#if annotation}}{{annotation}} {{/if}}{{type}} {{name}}{{/each}});
    {{/each}}
}
```

**Context:**
```typescript
{
    packageName: 'pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate',
    entityPackage: 'pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate',
    entityName: 'Execution',
    repositoryName: 'ExecutionRepository',
    customMethods: [
        {
            query: 'select e.aggregateId from Execution e where e.state != \'DELETED\'',
            returnType: 'Set<Integer>',
            name: 'findActiveExecutionIds',
            parameters: []
        }
    ]
}
```

**Generated Java:**
```java
package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import java.util.*;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Integer> {
    @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
    Set<Integer> findActiveExecutionIds();
}
```

## Template Best Practices

### 1. Keep Templates Simple

**Bad:**
```handlebars
{{#each fields}}
{{#if (and (eq type "String") (not (eq name "id")))}}
private {{type}} {{name}};
{{else if (eq type "Integer")}}
private {{type}} {{name}};
{{/if}}
{{/each}}
```

**Good:**
```handlebars
{{#each fields}}
private {{type}} {{name}};
{{/each}}
```

Filter and transform data in the generator, not the template.

### 2. Use Partials for Repetition

**Bad:**
```handlebars
public class UserDto {
    private Integer id;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}

public class CourseDto {
    private Integer id;
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
}
```

**Good:**
```handlebars
{{> dto-class name="UserDto" fields=userFields}}
{{> dto-class name="CourseDto" fields=courseFields}}
```

### 3. Comment Template Parameters

```handlebars
{{!--
  Service Implementation Template

  Required Context:
  - packageName: string - Package for this service
  - className: string - Service class name
  - methods: Method[] - Service methods to generate
  - dependencies: Dependency[] - Constructor dependencies

  Optional Context:
  - annotations: string[] - Class-level annotations
  - implements: string[] - Interfaces to implement
--}}
```

### 4. Handle Missing Data Gracefully

```handlebars
{{#if imports}}
{{imports}}

{{/if}}

{{#each methods}}
public {{returnType}} {{name}}({{parameters}}) {
    {{#if body}}
    {{body}}
    {{else}}
    // TODO: Implement
    throw new UnsupportedOperationException();
    {{/if}}
}
{{/each}}
```

### 5. Consistent Indentation

Use 4 spaces for Java code:

```handlebars
public class {{className}} {
    private {{type}} {{field}};

    public {{type}} get{{capitalize field}}() {
        return {{field}};
    }

    public void set{{capitalize field}}({{type}} {{field}}) {
        this.{{field}} = {{field}};
    }
}
```

## Debugging Templates

### Template Rendering Errors

**Error message:**
```
Error: Template rendering failed for entity/factory-interface.hbs
Context: {"packageName":"...","aggregateName":"User"}
Missing variable: createMethodParams
```

**Fix:**
1. Check context building in generator
2. Ensure all variables used in template are in context
3. Use `{{json this}}` to dump context

### Dev Mode

Enable development mode to reload templates on change:

```typescript
const templateManager = TemplateManager.getInstance({ enableDevMode: true });
```

**Benefits:**
- No need to rebuild after template changes
- Faster iteration during development

### Template Debugging Helpers

```handlebars
{{!-- Dump entire context --}}
Context: {{json this}}

{{!-- Dump specific variable --}}
AggregateSpecific variable:
```

{{json aggregate}}

{{!-- Check if variable exists --}}
{{#if variableName}}
Variable exists: {{variableName}}
{{else}}
Variable is missing!
{{/if}}
```

## Next Steps

Explore generator implementations:

- **[08-Microservices-Generators](08-Microservices-Generators.md)** - Entity, Service, Repository generators
- **[06-Generator-System](06-Generator-System.md)** - Generator architecture
- **[10-Adding-DSL-Features](10-Adding-DSL-Features.md)** - Creating new generators and templates

---

**Previous:** [06-Generator-System](06-Generator-System.md) | **Next:** [08-Microservices-Generators](08-Microservices-Generators.md)
