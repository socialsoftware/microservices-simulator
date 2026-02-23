# Adding Features

This chapter provides a step-by-step guide for extending the Nebula DSL with new features, from grammar modifications to generator implementation and testing.

## Overview

Adding a new DSL feature involves:

1. **Grammar modification** -- define syntax in `nebula.langium`
2. **Parser regeneration** -- `npm run langium:generate`
3. **Validator implementation** -- add semantic checks (optional)
4. **Generator creation** -- implement code generation
5. **Template creation** -- define output structure (if needed)
6. **Testing** -- verify end-to-end
7. **Documentation** -- update guides

## Example: Adding Default Values

A complete walkthrough using default values (already implemented, but instructive as a pattern).

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
    LITERAL | STRING | QualifiedName | 'true' | 'false' | 'null';
```

### Step 2: Regenerate Parser

```bash
cd dsl/nebula
npm run langium:generate
```

This updates `src/language/generated/ast.ts`:
```typescript
export interface Property extends AstNode {
    isFinal?: boolean;
    type: Type;
    name: string;
    defaultValue?: string;  // New field
}
```

### Step 3: Update Validators (Optional)

**File:** `src/language/nebula-validator.ts`

```typescript
@check('Property')
checkDefaultValueTypeCompatibility(property: Property, accept: ValidationAcceptor): void {
    if (property.defaultValue) {
        const type = property.type;
        if (type.$type === 'PrimitiveType' && type.typeName === 'Boolean') {
            if (property.defaultValue !== 'true' && property.defaultValue !== 'false') {
                accept('error', 'Boolean property must have true or false default', {
                    node: property, property: 'defaultValue'
                });
            }
        }
    }
}
```

### Step 4: Update Entity Generator

**File:** `src/cli/generators/microservices/entity/fields.ts`

```typescript
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
```

### Step 5: Rebuild and Test

```bash
npm run build
./bin/cli.js generate ../abstractions/answers/
```

**Test DSL:**
```nebula
Root Entity User {
    Boolean active = true
    Integer count = 0
    AggregateState state = AggregateState.ACTIVE
}
```

**Generated Java:**
```java
private Boolean active = true;
private Integer count = 0;
private AggregateState state = AggregateState.ACTIVE;
```

## Example: Adding Method Annotations

### Step 1: Grammar

```langium
ServiceMethod:
    (annotations+=Annotation)*
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?;
```

### Step 2: Generator

```typescript
private buildAnnotations(annotations: Annotation[]): string {
    return annotations.map(ann => {
        if (ann.values && ann.values.length > 0) {
            const values = ann.values.map(v =>
                v.key ? `${v.key} = ${v.value}` : v.value
            ).join(', ');
            return `@${ann.name}(${values})`;
        }
        return `@${ann.name}`;
    }).join('\n');
}
```

## Common Patterns for New Features

### Pattern 1: Adding a New Aggregate Element

1. **Grammar:** Add to `AggregateElement` union and define the element rule
2. **Generator:** Create a new generator class
3. **Feature facade:** Create or update a facade
4. **Template:** Create a Handlebars template
5. **Registry:** Register in `generator-registry.ts`

### Pattern 2: Adding a New Property Modifier

1. **Grammar:** Add optional flag to `Property` rule
2. **Entity generator:** Handle the flag in field/annotation generation
3. **DTO generator:** Handle in DTO field generation if needed

### Pattern 3: Adding a New Expression Type

1. **Grammar:** Add to the expression hierarchy
2. **Invariant compiler:** Handle the new expression type in `convertDslToJava()`

## Testing Strategy

### Manual Testing (End-to-End)

```bash
# 1. Create/modify test abstraction
# 2. Rebuild DSL
npm run langium:generate && npm run build

# 3. Generate code
./bin/cli.js generate ../abstractions/answers/

# 4. Compile generated code
cd ../../applications/answers && mvn clean compile

# 5. Run tests
mvn clean -Ptest-sagas test
```

### Debugging

**Enable verbose logging in generators:**
```typescript
console.log('[MyGenerator] Input:', JSON.stringify(input, null, 2));
```

**Dump template context:**
```handlebars
{{json this}}
```

**Inspect AST:**
```typescript
console.log('AST:', JSON.stringify(aggregate, null, 2));
```

## Checklist for New Features

- [ ] Grammar updated in `nebula.langium`
- [ ] Parser regenerated (`npm run langium:generate`)
- [ ] Validators added/updated if needed
- [ ] Generator implemented
- [ ] Template created (if needed)
- [ ] Code rebuilt (`npm run build`)
- [ ] Manual testing passed (generate + compile)
- [ ] Documentation updated
- [ ] Example added to abstractions

## Common Pitfalls

### Forgetting to Regenerate Parser

**Symptom:** TypeScript errors about missing properties

**Fix:** `npm run langium:generate && npm run build`

### Incorrect Grammar Cardinality

```langium
// WRONG: defaultValue is required
Property: type=Type name=ID defaultValue=STRING;

// CORRECT: defaultValue is optional
Property: type=Type name=ID (defaultValue=STRING)?;
```

### Not Handling Missing Data in Templates

```handlebars
{{!-- WRONG: breaks if no default --}}
private {{type}} {{name}} = {{defaultValue}};

{{!-- CORRECT: conditional --}}
private {{type}} {{name}}{{#if defaultValue}} = {{defaultValue}}{{/if}};
```

### Not Validating Cross-References

```typescript
// WRONG: assumes exists
const referencedAggregate = models[0].aggregates.find(...);

// CORRECT: check existence
const referencedAggregate = findAggregateInModels(aggregateName);
if (!referencedAggregate) {
    throw new Error(`Aggregate '${aggregateName}' not found`);
}
```

### Templates Not Updating

Templates are not TypeScript files. After editing, run `npm run build` to copy them to `out/`.

---

**Previous:** [04-Template-System](04-Template-System.md) | **Next:** [06-Patterns-Reference](06-Patterns-Reference.md)
