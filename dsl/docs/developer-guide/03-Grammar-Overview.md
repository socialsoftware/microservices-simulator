# Grammar Overview

The Nebula DSL is built on **Langium**, a language engineering framework that provides parser generation, AST construction, and IDE integration. This chapter explains how the grammar works, its design philosophy, and key grammar rules.

## What is Langium?

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
| Validation | Custom code | Declarative + validators |

## Grammar File Structure

### Location

`dsl/nebula/src/language/nebula.langium` (500 lines)

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
WebAPIEndpoints: ...
ServiceDefinition: ...
Functionalities: ...

// Type system
Type: ...
PrimitiveType: ...
EntityType: ...
ListType: ...
SetType: ...

// Expressions
Expression: ...
BooleanExpression: ...
Comparison: ...

// Terminals
terminal ID: ...
terminal STRING: ...
terminal LITERAL: ...
```

## Design Philosophy

### 1. Convention Over Configuration

**Smart defaults reduce verbosity:**

```nebula
// Service name is optional - auto-generated from aggregate
Service {
    @Transactional
}
```

```nebula
// Default values use intuitive syntax
Root Entity User {
    Boolean active = true
    AggregateState state = AggregateState.ACTIVE
}
```

### 2. Type Safety

**Strong typing throughout:**

```nebula
// Type inference for cross-aggregate references
Entity ExecutionCourse from Course {
    map name as courseName      // Type inferred: String
    map credits as courseCredits // Type inferred: Integer
}
```

**Collection type safety:**
```nebula
Root Entity Quiz {
    Set<ExecutionUser> participants  // Enforced at parse time
    List<Question> questions         // No mixed types allowed
}
```

### 3. Declarative Syntax

**Focus on WHAT, not HOW:**

```nebula
// Declare invariants - generator handles enforcement
invariants {
    check nameNotBlank { name.length() > 0 }
        error "Name cannot be blank"
}
```

```nebula
// Declare events - generator handles pub/sub infrastructure
Events {
    publish UserDeleted {
        Integer userId
        String username
    }
}
```

### 4. Modern, Clean Syntax

**No semicolons (removed in Week 1):**
```nebula
Root Entity User {
    String name
    UserRole role
}
```

**Intuitive operators:**
```nebula
Boolean active = true        // Not: default true
Integer count = 0            // Not: default value 0
```

## Key Grammar Rules

### Entry Point: Model

```langium
entry Model:
    (aggregates+=Aggregate | sharedEnums+=SharedEnums)*
    (exceptions=ExceptionMessages)?;
```

**What this means:**
- A `.nebula` file contains zero or more `Aggregate` or `SharedEnums` blocks
- Optionally, one `exceptions` block at the end
- Order doesn't matter (aggregates and enums can be interleaved)

**Example:**
```nebula
SharedEnums {
    enum UserRole {
        STUDENT, TEACHER, ADMIN
    }
}

Aggregate User {
    Root Entity User {
        UserRole role
    }
}

exceptions {
    USER_NOT_FOUND: "User with ID %d not found"
}
```

### Aggregate Definition

```langium
Aggregate:
    (annotations+=Annotation)*
    'Aggregate' name=ID '{'
        (generateCrud?='@GenerateCrud')?
        (aggregateElements+=AggregateElement)*
    '}';
```

**Key features:**
- `annotations+=Annotation` - Zero or more annotations (e.g., `@MyAnnotation`)
- `generateCrud?='@GenerateCrud'` - Optional CRUD generation flag
- `aggregateElements+=AggregateElement` - Zero or more elements (entities, events, etc.)

**Example:**
```nebula
@SomeAnnotation
Aggregate User {
    @GenerateCrud

    Root Entity User { ... }
    Events { ... }
    Repository { ... }
}
```

### Aggregate Elements

```langium
AggregateElement:
    Entity | Method | Workflow | Repository | Events |
    References | WebAPIEndpoints | ServiceDefinition | Functionalities;
```

**Cardinality constraints** (enforced by validators, not grammar):
- **Multiple allowed:** Entity, Method, Workflow
- **Only one allowed:** Repository, Events, References, WebAPIEndpoints, ServiceDefinition, Functionalities

**Why not enforce in grammar?**
- Grammar would become complex with lookahead
- Validator provides better error messages
- Allows parsing even with errors (better IDE experience)

### Entity Definition

```langium
Entity:
    (isRoot?='Root')? (generateDto?='Dto')? 'Entity' name=ID
    ('from' aggregateRef=ID)?
    (
        '{'
            (properties+=Property | fieldMappings+=DtoFieldMapping)*
            ('invariants' '{'
                invariants+=Invariant*
            '}')?
        '}'
    )?;
```

**Key features:**

1. **Root entity marker:**
   ```nebula
   Root Entity User { ... }
   ```

2. **DTO generation flag:**
   ```nebula
   Dto Entity CourseDto { ... }
   ```

3. **Cross-aggregate reference:**
   ```nebula
   Entity ExecutionCourse from Course { ... }
   ```

4. **Property or field mapping:**
   ```nebula
   Entity User {
       String name            // Property
       Integer age            // Property
   }

   Entity ExecutionCourse from Course {
       map name as courseName     // Field mapping
       map type as courseType     // Field mapping
   }
   ```

5. **Invariants block:**
   ```nebula
   Root Entity User {
       String name

       invariants {
           check nameNotBlank { name.length() > 0 }
               error "Name cannot be blank"
       }
   }
   ```

### Property Definition

```langium
Property:
    (isFinal?='final')? type=Type name=ID (',' names+=ID)*
    ('=' defaultValue=PropertyDefaultValue)?
    (dtoExclude?='dto-exclude')?;
```

**Features:**

1. **Immutable fields:**
   ```nebula
   final UserRole role
   ```

2. **Multiple properties of same type:**
   ```nebula
   String firstName, lastName, email
   ```

3. **Default values:**
   ```nebula
   Boolean active = true
   Integer count = 0
   AggregateState state = AggregateState.ACTIVE
   ```

4. **DTO exclusion:**
   ```nebula
   String internalId dto-exclude
   ```

### Cross-Aggregate Field Mapping

```langium
DtoFieldMapping:
    'map' dtoField=DtoFieldPath 'as' entityField=ID;

DtoFieldPath:
    parts+=ID ('.' parts+=ID)*;
```

**Simple mapping:**
```nebula
Entity ExecutionCourse from Course {
    map name as courseName
    map type as courseType
}
```

**Dotted path (extract pattern - future work):**
```nebula
Entity QuizWithQuestions from Quiz {
    map questions.aggregateId as questionIds
}
```

**Type inference:**
The generator resolves types by:
1. Finding the `Course` aggregate
2. Getting its root entity
3. Finding the `name` property
4. Using its type for `courseName`

### Type System

```langium
Type:
    BaseType | ListType | SetType | OptionalType;

BaseType:
    PrimitiveType | BuiltinType | EntityType;

PrimitiveType:
    typeName=('Integer' | 'Long' | 'Float' | 'Double' |
              'String' | 'Boolean' | 'LocalDateTime' | 'Object');

BuiltinType:
    typeName=('UnitOfWork' | 'AggregateState');

EntityType:
    type=[Entity:QualifiedName];

ListType:
    'List' '<' elementType=BaseType '>';

SetType:
    'Set' '<' elementType=BaseType '>';

OptionalType:
    'Optional' '<' elementType=BaseType '>';
```

**Examples:**
```nebula
String name                      // PrimitiveType
UserRole role                    // EntityType (enum)
ExecutionCourse course           // EntityType (entity)
Set<ExecutionUser> users         // SetType<EntityType>
List<Integer> scores             // ListType<PrimitiveType>
Optional<LocalDateTime> deadline // OptionalType<PrimitiveType>
```

**Type constraints:**
- No nested collections: `List<List<Integer>>` ❌
- No void in collections: `Set<void>` ❌
- Element type must be BaseType (primitive, builtin, or entity)

### Events

```langium
Events:
    'Events' '{'
        (publishedEvents+=PublishedEvent)*
        (subscribedEvents+=SubscribedEvent)*
        (interInvariants+=InterInvariant)*
    '}';

PublishedEvent:
    'publish' name=ID '{'
        fields+=EventField*
    '}';

SubscribedEvent:
    'subscribe' eventType=ID
    ('from' sourceAggregate=ID)?
    ('routing' '(' routingIdExpr=Expression (',' routingVersionExpr=Expression)? ')')?
    (
        '{'
            conditions+=EventCondition*
        '}'
    )?;
```

**Published event:**
```nebula
publish UserDeleted {
    Integer userId
    String username
    UserRole role
}
```

**Subscribed event (simple):**
```nebula
subscribe CourseDeletedEvent
```

**Subscribed event (with source):**
```nebula
subscribe UserDeletedEvent from User
```

**Subscribed event (with routing):**
```nebula
subscribe UserUpdatedEvent from User routing (users.userAggregateId)
```

**Subscribed event (with condition):**
```nebula
subscribe CourseDeletedEvent from Course {
    course.courseAggregateId == event.aggregateId
}
```

**Inter-invariants (reference constraints):**
```nebula
interInvariant USERS_EXIST {
    subscribe UserDeletedEvent from User {
        users.userAggregateId == event.aggregateId
    }
    subscribe UserUpdatedEvent from User {
        users.userAggregateId == event.aggregateId
    }
}
```

### Repository Definition

```langium
Repository:
    'Repository' '{'
        repositoryMethods+=RepositoryMethod*
    '}';

RepositoryMethod:
    ('@Query' '(' query=STRING ')')?
    returnType=RepositoryReturnType
    name=ID
    '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')';
```

**Custom query:**
```nebula
Repository {
    @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
    Set<Integer> findActiveExecutionIds()

    @Query("select a from Answer a where a.quiz.quizAggregateId = :quizId")
    List<Answer> findByQuizId(Integer quizId)
}
```

**Spring Data method (query derived from name):**
```nebula
Repository {
    Optional<User> findByUsername(String username)
    List<User> findByRole(UserRole role)
}
```

### Invariants

```langium
Invariant:
    'check' name=ID '{'
        conditions+=InvariantCondition*
    '}'
    'error' errorMessage=STRING;

InvariantCondition:
    expression=Expression;

Expression:
    BooleanExpression;

BooleanExpression:
    {infer BooleanExpression} left=Comparison
    (op=("||"|"&&"|"OR"|"AND") right=Comparison)*;

Comparison:
    {infer Comparison} left=Addition
    (op=("<" | ">" | "<=" | ">=" | "==" | "!=") right=Addition)*;
```

**Examples:**
```nebula
invariants {
    check nameNotBlank { name.length() > 0 }
        error "Name cannot be blank"

    check roleNotNull { role != null }
        error "Role is required"

    check dateOrdering {
        availableDate.isBefore(conclusionDate) &&
        conclusionDate.isBefore(resultsDate)
    }
        error "Dates must be in chronological order"
}
```

**Supported expression types:**
- Comparisons: `<`, `>`, `<=`, `>=`, `==`, `!=`
- Boolean logic: `&&`, `||`, `!`
- Method calls: `name.length()`, `date.isBefore(other)`
- Collection operations: `users.size()`, `users.isEmpty()`
- Property access: `course.name`, `user.role`

### References (Referential Integrity)

```langium
References:
    'References' '{'
        (constraints+=ReferenceConstraint)*
    '}';

ReferenceConstraint:
    fieldName=ID '->' targetAggregate=ID '{'
        'onDelete' ':' action=ReferenceAction
        'message' ':' message=STRING
    '}';

ReferenceAction returns string:
    'prevent' | 'cascade' | 'setNull';
```

**Example:**
```nebula
References {
    course -> Course {
        onDelete: prevent
        message: "Cannot delete course that has executions"
    }

    creator -> User {
        onDelete: setNull
        message: "Creator deleted"
    }
}
```

**Actions:**
- `prevent` - Throw exception if referenced aggregate is deleted
- `cascade` - Delete this aggregate when referenced aggregate is deleted
- `setNull` - Set reference to null when referenced aggregate is deleted

### Service Definition

```langium
ServiceDefinition:
    'Service' (name=ID)? '{'
        (transactional?='@Transactional')?
        ('methods' '{'
            serviceMethods+=ServiceMethod*
        '}')?
    '}';

ServiceMethod:
    (annotations+=Annotation)*
    name=ID '(' (parameters+=Parameter (',' parameters+=Parameter)*)? ')'
    (':' returnType=ReturnType)?;
```

**Example:**
```nebula
Service UserService {
    @Transactional

    methods {
        activateUser(Integer userId, UnitOfWork unitOfWork): UserDto
        deactivateUser(Integer userId, UnitOfWork unitOfWork): UserDto
    }
}
```

**If name is omitted:**
```nebula
Service {
    @Transactional
}
// Generated name: UserService (from aggregate name)
```

## Grammar Evolution (Week 1-4 Improvements)

### Week 1: Boilerplate Reduction

**Removed imports:**
```nebula
// Before
import shared-enums;

Aggregate User { ... }

// After
Aggregate User { ... }
```

**Removed semicolons:**
```nebula
// Before
String name;
Integer age;

// After
String name
Integer age
```

**Simplified defaults:**
```nebula
// Before
Boolean active default true;

// After
Boolean active = true
```

**Optional service names:**
```nebula
// Before
Service UserService { }

// After
Service { }  // Auto-generated: UserService
```

### Week 2: Type Inference

**Old syntax (explicit types):**
```nebula
Entity ExecutionCourse uses Course {
    String courseName from name
    CourseType courseType from type
}
```

**New syntax (inferred types):**
```nebula
Entity ExecutionCourse from Course {
    map name as courseName      // Type inferred: String
    map type as courseType      // Type inferred: CourseType
}
```

**Mandatory error messages:**
```nebula
// Before (optional)
check nameNotBlank { name.length() > 0 }

// After (required)
check nameNotBlank { name.length() > 0 }
    error "Name cannot be blank"
```

## Langium Concepts in Nebula

### Cross-References

**Grammar:**
```langium
Entity:
    'Entity' name=ID ('from' aggregateRef=ID)?;
```

**Usage:**
```nebula
Entity ExecutionCourse from Course { ... }
```

**How it works:**
1. Parser sees `Course` after `from`
2. Langium creates a reference to an aggregate named `Course`
3. At build time, Langium resolves the reference
4. If `Course` doesn't exist, validation error is raised

### Inferred Types

**Grammar:**
```langium
BooleanExpression:
    {infer BooleanExpression} left=Comparison
    (op=("||"|"&&") right=Comparison)*;
```

**What `{infer BooleanExpression}` does:**
- Creates an AST node of type `BooleanExpression`
- Even if no operator is present, still wraps in `BooleanExpression`
- Enables uniform handling in TypeScript

**Generated TypeScript:**
```typescript
export interface BooleanExpression extends Expression {
    left: Comparison;
    op?: "||" | "&&" | "OR" | "AND";
    right?: Comparison;
}
```

### Optional vs. Required

**Optional (zero or one):**
```langium
Entity:
    (isRoot?='Root')?  // Optional 'Root' keyword
    'Entity' name=ID;
```

**Required (exactly one):**
```langium
Aggregate:
    'Aggregate' name=ID '{'  // name is required
        ...
    '}';
```

**Zero or more:**
```langium
Model:
    aggregates+=Aggregate*;  // * means zero or more
```

**One or more:**
```langium
PublishedEvent:
    'publish' name=ID '{'
        fields+=EventField+  // + means one or more
    '}';
```

### Hidden Tokens

```langium
hidden terminal WS:
    /\s+/;

hidden terminal ML_COMMENT:
    /\/\*([^*]|\*+[^*/])*\*+\//;

hidden terminal SL_COMMENT:
    /\/\/[^\n\r]*/;
```

**Effect:**
- Whitespace, comments ignored by parser
- No need to handle them in grammar rules
- Makes grammar cleaner and more readable

## Validation Beyond Grammar

Some constraints are enforced by validators (`nebula-validator.ts`) rather than grammar:

**Cardinality constraints:**
```typescript
// Only one Repository block allowed per aggregate
checkValidator.checkOneRepositoryPerAggregate(aggregate);
```

**Type constraints:**
```typescript
// Entity must have root entity
checkValidator.checkAggregateHasRootEntity(aggregate);
```

**Semantic constraints:**
```typescript
// Cross-reference must point to valid aggregate
checkValidator.checkEntityAggregateRefExists(entity);
```

**Why not in grammar?**
- Grammar would become too complex
- Better error messages from validators
- Allows incremental parsing (IDE support)
- Separation of syntactic vs. semantic validation

## Next Steps

Now that you understand the grammar:

- **[04-DSL-Features](04-DSL-Features.md)** - Explore all DSL capabilities in depth
- **[11-Grammar-Reference](11-Grammar-Reference.md)** - Complete syntax reference
- **[10-Adding-DSL-Features](10-Adding-DSL-Features.md)** - Learn how to extend the grammar

---

**Previous:** [02-Project-Structure](02-Project-Structure.md) | **Next:** [04-DSL-Features](04-DSL-Features.md)
