# Template System

This chapter covers Nebula's template-based code generation using Handlebars, including template organization, the Template Manager, helpers, partials, and context building.

## Why Handlebars?

**Handlebars** is a logic-less template engine that separates presentation (templates) from logic (generators).

**Benefits:**
1. **Separation of concerns** -- templates focus on structure, generators focus on logic
2. **Readability** -- templates look like the output they generate
3. **Maintainability** -- easy to modify generated code structure
4. **Reusability** -- partials and helpers promote DRY

## Template Manager

**File:** `src/cli/utils/template-manager.ts`

`TemplateManager` is a singleton that handles template loading, caching, and rendering.

```typescript
export class TemplateManager {
    private static instance: TemplateManager;
    private templateCache = new Map<string, TemplateCacheEntry>();

    // Singleton access
    static getInstance(config?: TemplateManagerConfig): TemplateManager;

    // Core methods
    loadTemplate(templatePath: string): HandlebarsTemplateDelegate<any>;
    renderTemplate(templatePath: string, context: any): string;
    loadRawTemplate(templatePath: string): string;

    // Cache management
    clearCache(): void;
    invalidateTemplate(templatePath: string): void;
    getCacheStats(): { size: number; hitRate: number; templates: string[] };

    // Development mode - reload on file change
    setDevMode(enabled: boolean): void;
}
```

Templates are compiled once and cached. Subsequent renders skip parsing for better performance.

### Usage in Generators

```typescript
// Generators access via base class
protected renderTemplate(templatePath: string, context: any): string {
    return this.templateManager.renderTemplate(templatePath, context);
}

// Example
const code = this.renderTemplate('entity/factory-interface.hbs', {
    packageName: '...user.aggregate',
    aggregateName: 'User',
    dtoName: 'UserDto'
});
```

## Template Organization

```
src/cli/templates/
├── entity/
│   ├── factory-interface.hbs      # Factory interface
│   └── aggregate-base.hbs         # Aggregate wrapper class
├── repository/
│   └── repository-interface.hbs   # Spring Data repository
├── service/
│   ├── service-definition.hbs     # Service interface
│   └── service-implementation.hbs
├── web/
│   ├── controller.hbs             # REST controller
│   ├── request-dtos.hbs
│   └── response-dtos.hbs
├── events/
│   ├── published-event.hbs        # Event class
│   ├── event-subscription.hbs     # EventSubscription impl
│   ├── event-handler.hbs
│   └── event-handling.hbs         # Event handling router
├── saga/
│   ├── saga-functionality.hbs
│   ├── saga-coordination.hbs
│   └── saga-aggregate.hbs
├── coordination/
│   └── functionalities.hbs
├── config/
│   ├── pom.hbs                    # Maven POM
│   ├── application.hbs            # application.yml
│   └── configuration.hbs          # Spring @Configuration
└── _partials/                     # Reusable fragments
    └── dto-class.hbs
```

**Naming conventions:**
- Template files: `kebab-case.hbs`
- Partials: in `_partials/` directory
- Context variables: `camelCase`

## Template Syntax

### Basic Interpolation

```handlebars
package {{packageName}};

public class {{className}} {
    private {{type}} {{fieldName}};
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
```

### Loops

```handlebars
{{#each fields}}
private {{type}} {{name}};
{{/each}}
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

## Handlebars Helpers

Nebula registers custom helpers in the TemplateManager:

### Comparison Helpers
```handlebars
{{#if (eq type "String")}}...{{/if}}
{{#if (ne type "void")}}...{{/if}}
{{#if (gt count 0)}}...{{/if}}
```

### Boolean Logic
```handlebars
{{#if (and isRoot hasInvariants)}}
@Override
public void verifyInvariants() { ... }
{{/if}}
```

### String Manipulation
```handlebars
public {{capitalize returnType}} get{{capitalize fieldName}}() {
    return {{fieldName}};
}
```

### Registered Helpers

```typescript
// Comparison
Handlebars.registerHelper('eq', (a, b) => a === b);
Handlebars.registerHelper('ne', (a, b) => a !== b);
Handlebars.registerHelper('lt', (a, b) => a < b);
Handlebars.registerHelper('gt', (a, b) => a > b);

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
```

## Partials

Partials are reusable template fragments in the `_partials/` directory.

### Defining a Partial

**File:** `src/cli/templates/_partials/dto-class.hbs`

```handlebars
{{!--
  Shared DTO Class Template
  Parameters:
  - name: Class name
  - fields: Array of {name, type, required}
--}}
public class {{name}} {
    {{#each fields}}
    private {{type}} {{name}};
    {{/each}}

    public {{name}}() {}

    {{#each fields}}
    public {{type}} get{{capitalize name}}() { return {{name}}; }
    public void set{{capitalize name}}({{type}} {{name}}) { this.{{name}} = {{name}}; }
    {{/each}}
}
```

### Using Partials

```handlebars
{{> dto-class name="CreateUserRequestDto" fields=requestFields}}
{{> dto-class name="UserDto" fields=dtoFields}}
```

## Context Building

Generators build structured context objects for templates. The pattern is:

1. Extract data from AST
2. Transform and compute derived values
3. Build flat context object
4. Render template

```typescript
export class FactoryGenerator extends GeneratorBase {
    async generateFactory(aggregate: Aggregate, options: GenerationOptions): Promise<string> {
        // 1. Extract data
        const rootEntity = this.findRootEntity(aggregate);
        const aggregateName = aggregate.name;

        // 2. Build context
        const context = {
            packageName: this.generatePackageName(options.projectName, aggregateName.toLowerCase(), 'aggregate'),
            imports: this.buildImports(aggregate, options),
            aggregateName: aggregateName,
            createMethodParams: `Integer aggregateId, ${aggregateName}Dto dto`,
            dtoName: `${aggregateName}Dto`,
            lowerAggregateName: this.toCamelCase(aggregateName)
        };

        // 3. Render
        return this.renderTemplate('entity/factory-interface.hbs', context);
    }
}
```

### Context Best Practices

1. **Flat structure** -- avoid deep nesting
2. **Meaningful names** -- `aggregateName` not `an`
3. **Pre-computed values** -- do logic in generator, not template
4. **Type safety** -- use TypeScript interfaces for context

## Real-World Examples

### Factory Interface

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

**Output:**
```java
package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

public interface UserFactory {
    User createUser(Integer aggregateId, UserDto dto);
    User createUserFromExisting(User existingUser);
    UserDto createUserDto(User user);
}
```

### Repository Interface

**Template:** `repository/repository-interface.hbs`
```handlebars
package {{packageName}};

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface {{repositoryName}} extends JpaRepository<{{entityName}}, Integer> {
    {{#each customMethods}}
    {{#if query}}
    @Query("{{query}}")
    {{/if}}
    {{returnType}} {{name}}({{#each parameters}}{{#if @index}}, {{/if}}{{type}} {{name}}{{/each}});
    {{/each}}
}
```

## Template Best Practices

### 1. Keep Templates Simple
Filter and transform data in the generator, not the template.

### 2. Use Partials for Repetition
Avoid duplicating template structures.

### 3. Comment Template Parameters
```handlebars
{{!--
  Required Context:
  - packageName: string
  - className: string
  - methods: Method[]
--}}
```

### 4. Handle Missing Data Gracefully
```handlebars
{{#if imports}}
{{imports}}
{{/if}}
```

### 5. Consistent Indentation
Use 4 spaces for Java code inside templates.

## Debugging Templates

### Dump Context
```handlebars
{{json this}}
```

### Check Variable Existence
```handlebars
{{#if variableName}}
Variable: {{variableName}}
{{else}}
Variable is missing!
{{/if}}
```

### Dev Mode
Enable development mode to reload templates without rebuilding:
```typescript
const templateManager = TemplateManager.getInstance({ enableDevMode: true });
```

### Common Errors

**"Missing variable: createMethodParams"** -- check that the generator includes all variables used by the template.

**Templates not updating** -- run `npm run build` to copy templates to `out/`.

---

**Previous:** [03-Generator-System](03-Generator-System.md) | **Next:** [05-Adding-Features](05-Adding-Features.md)
