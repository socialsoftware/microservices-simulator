# Grammar Internals

This chapter covers how the Langium framework works, the grammar file structure, AST types, validation, and how to modify the grammar.

## Langium Framework

**Langium** is a TypeScript-based framework for building domain-specific languages. It provides:

- **Parser generation** from grammar definitions
- **Type-safe AST** construction
- **Language server protocol** support for IDE integration
- **Validation** and semantic checking
- **Cross-referencing** between language elements

### Langium vs. Traditional Parser Generators

| Feature | Traditional (ANTLR, Yacc) | Langium |
|---------|---------------------------|---------|
| Language | Java, C | TypeScript |
| AST | Manual classes | Auto-generated interfaces |
| IDE Support | Manual LSP implementation | Built-in LSP |
| Type Safety | Runtime checks | Compile-time TypeScript |

## Grammar File

**File:** `src/language/nebula.langium` (~500 lines)

### High-Level Organization

```langium
grammar Nebula

// Entry point
entry Model:
    (aggregates+=Aggregate | sharedEnums+=SharedEnums)*
    (exceptions=ExceptionMessages)?;

// Top-level constructs
Aggregate: ...
SharedEnums: ...
ExceptionMessages: ...

// Aggregate elements
Entity: ...
Repository: ...
Events: ...

// Type system
Type: ...
PrimitiveType: ...
ListType: ...

// Expressions
Expression: ...
BooleanExpression: ...

// Terminals
terminal ID: ...
terminal STRING: ...
```

### Key Grammar Rules and Their AST Types

When you define a grammar rule, Langium auto-generates a TypeScript interface in `src/language/generated/ast.ts`.

**Grammar:**
```langium
Aggregate:
    (annotations+=Annotation)*
    'Aggregate' name=ID '{'
        (generateCrud?='@GenerateCrud')?
        (aggregateElements+=AggregateElement)*
    '}';
```

**Generated AST:**
```typescript
export interface Aggregate extends AstNode {
    name: string;
    annotations?: Annotation[];
    generateCrud?: boolean;
    aggregateElements: AggregateElement[];
    entities: Entity[];
}
```

**Grammar:**
```langium
Entity:
    (isRoot?='Root')? (generateDto?='Dto')? 'Entity' name=ID
    ('from' aggregateRef=ID)?
    '{'
        (properties+=Property | fieldMappings+=DtoFieldMapping)*
        ('invariants' '{' invariants+=Invariant* '}')?
    '}';
```

**Generated AST:**
```typescript
export interface Entity extends AstNode {
    name: string;
    isRoot?: boolean;
    generateDto?: boolean;
    aggregateRef?: string;
    properties: Property[];
    fieldMappings: DtoFieldMapping[];
    invariants: Invariant[];
}
```

## Langium Concepts

### Cross-References vs Plain ID Tokens

This is an important distinction:

**Plain ID** - just a string, no resolution:
```langium
Entity:
    'Entity' name=ID ('from' aggregateRef=ID)?;
```

Here `aggregateRef` is just a string. The generator resolves it manually via the model registry.

**Cross-reference** - Langium resolves the reference:
```langium
EntityType:
    type=[Entity:QualifiedName];
```

Here `type` is a reference to an `Entity` node. Langium resolves it at parse time and raises validation errors if the target doesn't exist.

### Inferred Types

```langium
BooleanExpression:
    {infer BooleanExpression} left=Comparison
    (op=("||"|"&&") right=Comparison)*;
```

`{infer BooleanExpression}` creates an AST node even if no operator is present, ensuring uniform handling in TypeScript.

### Optional vs Required vs Repeated

```langium
(isRoot?='Root')?                    // Optional boolean flag
'Aggregate' name=ID                  // Required field
(aggregateElements+=AggregateElement)*  // Zero or more
fields+=EventField+                     // One or more
```

### Hidden Tokens

```langium
hidden terminal WS: /\s+/;
hidden terminal ML_COMMENT: /\/\*([^*]|\*+[^*/])*\*+\//;
hidden terminal SL_COMMENT: /\/\/[^\n\r]*/;
```

Whitespace and comments are ignored by the parser - no need to handle them in grammar rules.

## Scope Provider

**File:** `src/language/nebula-scope-provider.ts`

The scope provider resolves cross-references between elements. For example, when an entity references another entity type, the scope provider determines which entities are visible.

## Validators

**File:** `src/language/nebula-validator.ts`

Validators enforce semantic constraints that are impractical to express in grammar:

**Structural constraints:**
```typescript
// Each aggregate must have exactly one root entity
checkAggregateHasRootEntity(aggregate);

// Only one Repository block per aggregate
checkOneRepositoryPerAggregate(aggregate);
```

**Semantic constraints:**
```typescript
// Cross-reference must point to valid aggregate
checkEntityAggregateRefExists(entity);
```

**Why validators instead of grammar?**
- Grammar would become too complex with lookahead
- Validators provide better error messages
- Allows incremental parsing (better IDE experience)
- Separates syntactic from semantic validation

## Modifying the Grammar

### Step-by-Step Process

1. **Edit grammar:** `src/language/nebula.langium`
2. **Regenerate parser:** `npm run langium:generate`
3. **Check generated AST:** `src/language/generated/ast.ts`
4. **Update validators** if needed
5. **Update generators** to handle new AST nodes
6. **Build:** `npm run build`

### Example: Adding a New Property Modifier

**Before:**
```langium
Property:
    (isFinal?='final')? type=Type name=ID;
```

**After:**
```langium
Property:
    (isFinal?='final')? (indexed?='@Indexed')?
    type=Type name=ID;
```

After running `npm run langium:generate`, the generated AST will include:
```typescript
export interface Property extends AstNode {
    isFinal?: boolean;
    indexed?: boolean;  // New field
    type: Type;
    name: string;
}
```

### Common Pitfalls

**Ambiguous grammar:**
```langium
// BAD: Parser can't distinguish between Entity and Property
AggregateElement: Entity | Property;
```

**Left recursion:**
```langium
// BAD: Infinite recursion
Expression: Expression op='+' Expression;

// GOOD: Use left-factored rules
Expression: Comparison (op=('+' | '-') right=Comparison)*;
```

**Forgetting to regenerate:**
After grammar changes, always run:
```bash
npm run langium:generate && npm run build
```

> For the complete grammar syntax reference, see the [User Guide](../user-guide/11-Reference.md).

---

**Previous:** [01-Architecture](01-Architecture.md) | **Next:** [03-Generator-System](03-Generator-System.md)
