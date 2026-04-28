# Grammar

This chapter explains how the Langium grammar defines the Nebula DSL syntax, how the AST is structured, and how validation works.

## Grammar File

The grammar is defined in `src/language/nebula.langium`. Langium reads this file and generates:
- `src/language/generated/ast.ts` — TypeScript interfaces for every AST node
- `src/language/generated/grammar.ts` — the parser
- `src/language/generated/module.ts` — Langium module registration

After editing the grammar, run `npm run langium:generate` to regenerate these files.

## Top-Level Structure

```
Model
├── Aggregate*
│   └── AggregateElement*
│       ├── Entity (Root, Dto, projection)
│       ├── Methods
│       ├── Repository
│       └── Events
├── SharedEnums*
├── SagaStatesBlock*
├── TopLevelWorkflow*
└── ExceptionMessages?
```

A `.nebula` file produces a `Model` node containing any combination of these top-level elements.

## Key Grammar Rules

### Aggregate and Entities

```langium
Aggregate:
    'Aggregate' name=ID '{' aggregateElements+=AggregateElement* '}';

AggregateElement:
    Entity | Methods | Repository | Events;

Entity:
    (isRoot?='Root')? (generateDto?='Dto')? 'Entity' name=ID
    ('from' aggregateRef=ID)?
    '{' (properties+=Property | sourceBlocks+=SourceBlock)* ('invariants' '{' invariants+=Invariant* '}')? '}';
```

- `isRoot` flag marks the aggregate root entity
- `generateDto` flag marks value-object entities
- `aggregateRef` references another aggregate for cross-aggregate projections
- `sourceBlocks` define field mappings from the referenced aggregate

### Methods

```langium
Methods:
    'Methods' '{' methods+=Method* '}';

Method:
    annotations+=Annotation* name=ID '(' parameters+=Parameter* ')' (':' returnType=ReturnType)?
    '{' (rawBody=STRING | actionBody=ActionMethodBody | queryBody=QueryMethodBody) '}';

ActionMethodBody:
    ('precondition' '{' preconditions+=ActionPrecondition* '}')?
    'action' '{' statements+=ActionStatement* '}'
    publishes+=PublishesClause*;

ActionStatement:
    CreateActionStatement | LoadActionStatement | FindActionStatement | AssignActionStatement;
```

Methods have three possible bodies:
- `ActionMethodBody` — imperative statements (create, load, find, assign)
- `QueryMethodBody` — delegates to a repository method
- `rawBody` — raw string (escape hatch)

### Events

```langium
Events:
    'Events' '{' publishedEvents+=PublishedEvent* subscribedEvents+=SubscribedEvent* '}';

SubscribedEvent:
    'subscribe' eventType=ID ('from' sourceAggregate=ID)?
    ('routing' '(' routingIdExpr=Expression ')')?
    '{' ('when' whenCondition=Expression)? (reactionBody=ActionMethodBody)? '}';
```

Subscriptions can have:
- `when` — match condition (which instances react to this event)
- `reactionBody` — action body to execute (uses the same `ActionMethodBody` as methods)

### Workflows

```langium
TopLevelWorkflow:
    'Workflow' name=ID '{' ('input' '{' inputs+=WorkflowInput* '}')? workflowSteps+=TopLevelWorkflowStep+ '}';

TopLevelWorkflowStep:
    'step' name=ID '{'
        'action' ':' action=WorkflowActionCall
        ('compensate' ':' compensate=WorkflowActionCall)?
        ('lock' lockAggregate=ID '(' lockId=WorkflowCallArg ')' 'as' lockState=ID
            ('forbidden' '[' forbidden+=ID* ']')?)?
    '}';
```

### Invariants and Expressions

```langium
Invariant:
    conditions+=InvariantCondition ':' errorMessage=STRING;

Expression:
    BooleanExpression → Comparison → Addition → Multiplication → Prefixed → AtomicExpression;
```

The expression grammar supports boolean logic (`&&`, `||`, `!`), comparisons, arithmetic, method calls (`.length()`, `.isBefore()`), collection operations (`.size()`, `.isEmpty()`, `.allMatch()`), quantifiers (`forall`, `exists`), and property chains.

## AST Types

Every grammar rule generates a TypeScript interface in `generated/ast.ts`. For example:

```typescript
export interface Aggregate extends AstNode {
    readonly $type: 'Aggregate';
    aggregateElements: Array<AggregateElement>;
    annotations: Array<Annotation>;
    name: string;
}
```

Use `isEntity(node)`, `isMethod(node)` etc. (generated type guards) to narrow AST node types.

## Validation

Validators live in `src/language/validation/`. They're registered in `index.ts`:

```typescript
const checks: ValidationChecks<NebulaAstType> = {
    Model: validator.checkModel,
    Aggregate: validator.checkAggregate,
    Entity: validator.checkEntity,
    Property: validator.checkProperty,
    Method: validator.checkMethod,
    Invariant: validator.checkInvariant,
    RepositoryMethod: validator.checkRepositoryMethod,
    SubscribedEvent: validator.checkSubscribedEvent,
    TopLevelWorkflowStep: validator.checkTopLevelWorkflowStep,
};
```

Each validator checks semantics that the grammar can't express: entity naming rules, property type validity, invariant expression correctness, workflow step self-lock detection.

Validators run during `loadLanguageDocuments()` (Langium's document build phase), before code generation starts. Validation errors stop generation with a clear error message pointing to the file, line, and column.

## Adding a Grammar Rule

1. Add the rule to `nebula.langium`
2. Run `npm run langium:generate` — generates new AST types
3. Run `npm run build` — compiles TypeScript (will show errors if generators reference old types)
4. Update generators to consume the new AST node
5. Add a validator in `validation/` if the rule has semantic constraints
6. Register the validator in `validation/index.ts`

---

**Previous:** [02-Generation-Pipeline](02-Generation-Pipeline.md) | **Next:** [04-Generators-Templates](04-Generators-Templates.md)
