# Adding DSL Features

This chapter provides a step-by-step guide for extending the Nebula DSL with new features, from grammar modifications to generator implementation.

## Overview

Adding a new DSL feature involves:

1. **Grammar modification** - Define syntax
2. **Parser regeneration** - Generate TypeScript AST
3. **Validator implementation** - Add semantic checks
4. **Generator creation** - Implement code generation
5. **Template creation** - Define output structure
6. **Testing** - Verify end-to-end
7. **Documentation** - Update guides

## Example Feature: Adding Default Values

Let's walk through adding default values for properties (already implemented, but useful as example).

### Step 1: Modify Grammar

**File:** `src/language/nebula.langium`

**Before:**
```langium
Property:
    (isFinal?='final')? type=Type name=ID;
```

**After:**
```langium
Property:
    (isFinal?='final')? type=Type name=ID
    ('=' defaultValue=PropertyDefaultValue)?;

PropertyDefaultValue returns string:
    LITERAL
  | STRING
  | QualifiedName
  | 'true'
  | 'false'
  | 'null';
```

**What changed:**
- Added optional `('=' defaultValue=PropertyDefaultValue)?`
- Defined `PropertyDefaultValue` rule for valid default values

### Step 2: Regenerate Parser

```bash
cd dsl/nebula
npm run langium:generate
```

**What happens:**
- Langium reads `nebula.langium`
- Generates `src/language/generated/ast.ts`
- Updates AST interfaces

**Generated AST:**
```typescript
export interface Property extends AstNode {
    isFinal?: boolean;
    type: Type;
    name: string;
    defaultValue?: string;  // ← New field
}
```

### Step 3: Update Validators (Optional)

**File:** `src/language/nebula-validator.ts`

Add semantic validation if needed:

```typescript
export class NebulaValidator {
    @check('Property')
    checkDefaultValueTypeCompatibility(property: Property, accept: ValidationAcceptor): void {
        if (property.defaultValue) {
            const type = property.type;
            const defaultValue = property.defaultValue;

            // Check type compatibility
            if (type.$type === 'PrimitiveType') {
                const typeName = type.typeName;

                if (typeName === 'Boolean') {
                    if (defaultValue !== 'true' && defaultValue !== 'false') {
                        accept('error', 'Boolean property must have true or false default', {
                            node: property,
                            property: 'defaultValue'
                        });
                    }
                }

                if (typeName === 'Integer' || typeName === 'Long') {
                    if (!/^\d+$/.test(defaultValue)) {
                        accept('error', `${typeName} property must have numeric default`, {
                            node: property,
                            property: 'defaultValue'
                        });
                    }
                }
            }
        }
    }
}
```

### Step 4: Update Entity Generator

**File:** `src/cli/generators/microservices/entity/fields.ts`

Modify field generation to include default values:

```typescript
export class FieldGenerator {
    generateFields(entity: Entity): string[] {
        return entity.properties.map(prop => {
            const type = this.resolveJavaType(prop.type);
            const name = prop.name;
            const defaultVal = prop.defaultValue
                ? ` = ${this.convertDefaultValue(prop.defaultValue, prop.type)}`
                : '';

            return `private ${type} ${name}${defaultVal};`;
        });
    }

    private convertDefaultValue(dslValue: string, type: Type): string {
        // Convert DSL default value to Java default value

        if (type.$type === 'PrimitiveType') {
            const typeName = type.typeName;

            if (typeName === 'Boolean') {
                return dslValue; // 'true' or 'false' - same in Java
            }

            if (typeName === 'Integer' || typeName === 'Long') {
                return dslValue; // Numbers - same in Java
            }

            if (typeName === 'String') {
                return `"${dslValue}"`; // Add quotes
            }
        }

        if (type.$type === 'BuiltinType' && type.typeName === 'AggregateState') {
            // AggregateState.ACTIVE → same in Java
            return dslValue;
        }

        return 'null'; // Fallback
    }
}
```

### Step 5: Rebuild and Test

```bash
npm run build
./bin/cli.js generate ../abstractions/test/
```

**Test DSL:**
```nebula
Aggregate User {
    Root Entity User {
        String name
        Boolean active = true
        Integer count = 0
        AggregateState state = AggregateState.ACTIVE
    }
}
```

**Generated Java:**
```java
public class User extends Aggregate {
    private String name;
    private Boolean active = true;
    private Integer count = 0;
    private AggregateState state = AggregateState.ACTIVE;
}
```

### Step 6: Update Documentation

**File:** `docs/developer-guide/11-Grammar-Reference.md`

Add syntax documentation:

```markdown
### Default Values

Properties can have default values:

```nebula
Boolean active = true
Integer count = 0
String status = "pending"
AggregateState state = AggregateState.ACTIVE
```

Supported default values:
- Boolean: `true`, `false`
- Numeric: Integer literals
- String: String literals
- Enum: Qualified enum values
- Null: `null`
```

## Example Feature 2: Adding Method Annotations

### Step 1: Modify Grammar

```langium
ServiceMethod:
    (annotations+=Annotation)*  // ← Add this
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?;

Annotation:
    '@' name=ID ('(' (values+=AnnotationValue (',' values+=AnnotationValue)*)? ')')?;

AnnotationValue:
    key=ID '=' value=(STRING | ID | LITERAL) |
    value=(STRING | ID | LITERAL);
```

### Step 2: Regenerate Parser

```bash
npm run langium:generate
npm run build
```

### Step 3: Update Service Generator

```typescript
export class ServiceMethodGenerator {
    generateServiceMethod(method: ServiceMethod): string {
        const annotations = this.buildAnnotations(method.annotations);
        const signature = this.buildSignature(method);
        const body = this.buildBody(method);

        return `
${annotations}
${signature} {
${body}
}`;
    }

    private buildAnnotations(annotations: Annotation[]): string {
        return annotations.map(ann => {
            if (ann.values && ann.values.length > 0) {
                const values = ann.values.map(v => {
                    if (v.key) {
                        return `${v.key} = ${v.value}`;
                    }
                    return v.value;
                }).join(', ');
                return `@${ann.name}(${values})`;
            }
            return `@${ann.name}`;
        }).join('\n');
    }
}
```

### Step 4: Test

**DSL:**
```nebula
Service UserService {
    methods {
        @Transactional
        @Cacheable(value = "users", key = "#id")
        getUser(Integer id): UserDto

        @PreAuthorize("hasRole('ADMIN')")
        deleteUser(Integer id): void
    }
}
```

**Generated Java:**
```java
@Transactional
@Cacheable(value = "users", key = "#id")
public UserDto getUser(Integer id) {
    // Implementation
}

@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Integer id) {
    // Implementation
}
```

## Common Patterns for New Features

### Pattern 1: Adding New Aggregate Element

**Example:** Add `Configuration` block to aggregates

1. **Grammar:**
```langium
AggregateElement:
    Entity | Method | Workflow | Repository | Events |
    References | WebAPIEndpoints | ServiceDefinition |
    Functionalities | Configuration;  // ← Add

Configuration:
    'Configuration' '{'
        settings+=ConfigurationSetting*
    '}';

ConfigurationSetting:
    key=ID ':' value=(STRING | LITERAL);
```

2. **Generator:**
```typescript
export class ConfigurationGenerator extends GeneratorBase {
    async generateConfiguration(
        configuration: Configuration,
        options: GenerationOptions
    ): Promise<string> {
        const settings = this.extractSettings(configuration);
        return this.renderTemplate('config/configuration.hbs', {
            settings
        });
    }
}
```

3. **Template:**
```handlebars
@Configuration
public class {{aggregateName}}Configuration {
    {{#each settings}}
    @Value("${{{key}}}")
    private {{type}} {{key}};
    {{/each}}
}
```

### Pattern 2: Adding New Property Modifier

**Example:** Add `@Indexed` annotation for database indexing

1. **Grammar:**
```langium
Property:
    (isFinal?='final')? (indexed?='@Indexed')?  // ← Add
    type=Type name=ID;
```

2. **Entity Generator:**
```typescript
private buildFieldAnnotations(prop: Property): string[] {
    const annotations: string[] = [];

    if (prop.indexed) {
        annotations.push('@Indexed');
    }

    // ... other annotations

    return annotations;
}
```

3. **Test:**
```nebula
Root Entity User {
    @Indexed String username
    String name
}
```

### Pattern 3: Adding New Expression Type

**Example:** Add string concatenation in invariants

1. **Grammar:**
```langium
StringConcatExpression:
    {infer StringConcatExpression}
    left=AtomicExpression '+' right=AtomicExpression;

AtomicExpression:
    LiteralExpression |
    ParenthesizedExpression |
    MethodExpression |
    PropertyChainExpression |
    CollectionExpression |
    StringConcatExpression;  // ← Add
```

2. **Invariant Compiler:**
```typescript
private compileExpression(expr: Expression): string {
    if (expr.$type === 'StringConcatExpression') {
        const left = this.compileExpression(expr.left);
        const right = this.compileExpression(expr.right);
        return `${left} + ${right}`;
    }

    // ... handle other expression types
}
```

3. **Test:**
```nebula
invariants {
    check fullNameNotBlank {
        (firstName + " " + lastName).length() > 0
    }
        error "Full name cannot be blank"
}
```

## Testing Strategy

### Unit Tests (TypeScript)

```typescript
describe('DefaultValueGenerator', () => {
    it('should generate boolean default values', () => {
        const property: Property = {
            $type: 'Property',
            name: 'active',
            type: { $type: 'PrimitiveType', typeName: 'Boolean' },
            defaultValue: 'true'
        };

        const result = generator.generateField(property);
        expect(result).toBe('private Boolean active = true;');
    });

    it('should generate numeric default values', () => {
        const property: Property = {
            $type: 'Property',
            name: 'count',
            type: { $type: 'PrimitiveType', typeName: 'Integer' },
            defaultValue: '0'
        };

        const result = generator.generateField(property);
        expect(result).toBe('private Integer count = 0;');
    });
});
```

### Integration Tests (End-to-End)

```typescript
describe('Default Values Integration', () => {
    it('should generate entity with default values', async () => {
        // 1. Create test DSL file
        const dslContent = `
            Aggregate User {
                Root Entity User {
                    Boolean active = true
                    Integer count = 0
                }
            }
        `;
        await fs.writeFile('/tmp/test.nebula', dslContent);

        // 2. Generate code
        await CodeGenerator.generateCode('/tmp/test.nebula', {
            destination: '/tmp/output'
        });

        // 3. Verify generated code
        const generatedEntity = await fs.readFile(
            '/tmp/output/user/src/main/java/.../User.java',
            'utf-8'
        );

        expect(generatedEntity).toContain('private Boolean active = true;');
        expect(generatedEntity).toContain('private Integer count = 0;');
    });
});
```

### Manual Testing

```bash
# 1. Create test abstraction
cat > /tmp/test.nebula <<EOF
Aggregate User {
    Root Entity User {
        Boolean active = true
        Integer count = 0
    }
}
EOF

# 2. Generate code
./bin/cli.js generate /tmp/test.nebula -o /tmp/output

# 3. Verify compilation
cd /tmp/output/user
mvn clean compile

# 4. Check generated code
cat src/main/java/.../User.java
```

## Debugging New Features

### Enable Verbose Logging

```typescript
export class MyNewGenerator extends GeneratorBase {
    async generate(...) {
        console.log('[MyNewGenerator] Starting generation');
        console.log('[MyNewGenerator] Input:', JSON.stringify(input, null, 2));

        try {
            const result = this.doGenerate(input);
            console.log('[MyNewGenerator] Generated:', result);
            return result;
        } catch (error) {
            console.error('[MyNewGenerator] Error:', error);
            throw error;
        }
    }
}
```

### Use Template Debugging

```handlebars
{{!-- Dump context for debugging --}}
{{#if DEBUG}}
<!-- Context:
{{json this}}
-->
{{/if}}

{{!-- Your template content --}}
public class {{className}} {
    // ...
}
```

### AST Inspection

```typescript
// In generator
console.log('AST:', JSON.stringify(aggregate, null, 2));

// Or use Langium's built-in AST viewer
import { AstUtils } from 'langium';

const allNodes = AstUtils.streamAllContents(aggregate);
for (const node of allNodes) {
    console.log(`Node type: ${node.$type}`);
}
```

## Checklist for New Features

- [ ] Grammar updated in `nebula.langium`
- [ ] Parser regenerated (`npm run langium:generate`)
- [ ] Validators added/updated if needed
- [ ] Generator implemented
- [ ] Template created (if needed)
- [ ] Code rebuilt (`npm run build`)
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Manual testing passed
- [ ] Documentation updated
- [ ] Example added to abstractions
- [ ] Changelog updated

## Common Pitfalls

### 1. Forgetting to Regenerate Parser

**Symptom:** TypeScript errors about missing properties

**Fix:**
```bash
npm run langium:generate
npm run build
```

### 2. Incorrect Grammar Cardinality

**Wrong:**
```langium
Property:
    type=Type name=ID defaultValue=STRING;  // defaultValue is required
```

**Correct:**
```langium
Property:
    type=Type name=ID (defaultValue=STRING)?;  // defaultValue is optional
```

### 3. Not Handling Missing Data in Templates

**Wrong:**
```handlebars
private {{type}} {{name}} = {{defaultValue}};  {{!-- Breaks if no default --}}
```

**Correct:**
```handlebars
private {{type}} {{name}}{{#if defaultValue}} = {{defaultValue}}{{/if}};
```

### 4. Not Validating Cross-References

**Wrong:**
```typescript
const referencedAggregate = models[0].aggregates.find(...);  // Assumes exists
```

**Correct:**
```typescript
const referencedAggregate = findAggregateInModels(aggregateName);
if (!referencedAggregate) {
    throw new Error(`Aggregate '${aggregateName}' not found`);
}
```

## Next Steps

- **[11-Grammar-Reference](11-Grammar-Reference.md)** - Complete grammar syntax reference
- **[03-Grammar-Overview](03-Grammar-Overview.md)** - Grammar fundamentals
- **[06-Generator-System](06-Generator-System.md)** - Generator architecture

---

**Previous:** [09-Design-Patterns](09-Design-Patterns.md) | **Next:** [11-Grammar-Reference](11-Grammar-Reference.md)
