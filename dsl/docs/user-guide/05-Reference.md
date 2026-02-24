# Reference

Complete grammar syntax reference and CLI reference for the Nebula DSL.

## CLI Reference

### Generate Command

```bash
./bin/cli.js generate <abstractions-path> [options]
```

| Argument/Option | Description |
|-----------------|-------------|
| `<abstractions-path>` | Path to `.nebula` files (file or directory) |
| `-o, --output <dir>` | Output directory (default: `../../applications`) |
| `-d, --debug` | Enable debug mode with detailed error output |
| `-v, --verbose` | Enable verbose logging |
| `--no-validate` | Skip validation during generation |

The project name is automatically derived from the abstractions folder name (e.g., `../docs/examples/abstractions/06-tutorial/` produces project name `tutorial`).

### Examples

```bash
./bin/cli.js generate ../docs/examples/abstractions/06-tutorial/ -o ../docs/examples/generated
./bin/cli.js generate ../docs/examples/abstractions/06-tutorial/ -o ./output
./bin/cli.js generate ../abstractions/answers/ -o ../../applications/answers
./bin/cli.js generate ../abstractions/answers/ --debug --verbose
```

## Grammar Syntax Reference

### Model (Entry Point)

```langium
entry Model:
    (aggregates+=Aggregate | sharedEnums+=SharedEnums)*
    (exceptions=ExceptionMessages)?;
```

A `.nebula` file contains zero or more `Aggregate` or `SharedEnums` blocks, and optionally one `exceptions` block.

### Aggregate

```langium
Aggregate:
    (annotations+=Annotation)*
    'Aggregate' name=ID '{'
        (generateCrud?='@GenerateCrud')?
        (aggregateElements+=AggregateElement)*
    '}';
```

### Aggregate Elements

```langium
AggregateElement:
    Entity | Method | Workflow | Repository | Events |
    References | WebAPIEndpoints | ServiceDefinition | Functionalities;
```

| Element | Allowed Count |
|---------|---------------|
| Entity | 0 or more |
| Method | 0 or more |
| Workflow | 0 or more |
| Functionalities | 0 or more |
| Repository | 0 or 1 |
| Events | 0 or 1 |
| References | 0 or 1 |
| WebAPIEndpoints | 0 or 1 |
| ServiceDefinition | 0 or 1 |

### Entity

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

### Property

```langium
Property:
    (isFinal?='final')? type=Type name=ID (',' names+=ID)*
    ('=' defaultValue=PropertyDefaultValue)?
    (dtoExclude?='dto-exclude')?;
```

### Default Values

```langium
PropertyDefaultValue returns string:
    LITERAL | STRING | QualifiedName | AggregateStateQualified
    | 'true' | 'false' | 'null' | '[]';
```

### Field Mapping

```langium
DtoFieldMapping:
    'map' dtoField=DtoFieldPath 'as' entityField=ID;

DtoFieldPath:
    parts+=ID ('.' parts+=ID)*;
```

## Type System

### Type

```langium
Type:
    BaseType | ListType | SetType | OptionalType;

BaseType:
    PrimitiveType | BuiltinType | EntityType;
```

### Supported Data Types

| Category | Types |
|----------|-------|
| **Primitives** | `String`, `Integer`, `Long`, `Float`, `Double`, `Boolean`, `LocalDateTime`, `Object` |
| **Built-in** | `UnitOfWork`, `AggregateState` |
| **Entity** | Any defined entity or enum name |
| **Collections** | `List<T>`, `Set<T>`, `Optional<T>` |

**Constraints:**
- No nested collections: `List<List<Integer>>` is invalid
- Collection element type must be a BaseType

## Invariants

```langium
Invariant:
    'check' name=ID '{'
        conditions+=InvariantCondition*
    '}'
    'error' errorMessage=STRING;
```

### Expression Language

```langium
Expression:
    BooleanExpression;

BooleanExpression:
    {infer BooleanExpression} left=Comparison
    (op=("||"|"&&"|"OR"|"AND") right=Comparison)*;

Comparison:
    {infer Comparison} left=Addition
    (op=("<" | ">" | "<=" | ">=" | "==" | "!=") right=Addition)*;
```

### Operators

| Category | Operators |
|----------|-----------|
| **Boolean** | `&&`, `||`, `!`, `AND`, `OR`, `NOT` |
| **Comparison** | `<`, `>`, `<=`, `>=`, `==`, `!=` |
| **Arithmetic** | `+`, `-`, `*`, `/`, `%` |

### Method Expressions

| Type | Syntax | Example |
|------|--------|---------|
| **Unique check** | `collection.unique(field)` | `topics.unique(topicId)` |
| **Collection size** | `collection.size()` | `users.size()` |
| **Collection empty** | `collection.isEmpty()` | `questions.isEmpty()` |
| **Time before** | `date.isBefore(other)` | `startDate.isBefore(endDate)` |
| **Time after** | `date.isAfter(other)` | `deadline.isAfter(now())` |
| **String length** | `string.length()` | `name.length()` |
| **Quantifiers** | `forall x : coll \| cond` | `forall u : users \| u.active == true` |

## Events

### Published Events

```langium
PublishedEvent:
    'publish' name=ID '{'
        fields+=EventField*
    '}';

EventField:
    type=Type name=ID;
```

### Subscribed Events

```langium
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

### Event Condition Syntax

Conditions compare local aggregate state with event data:
```nebula
course.courseAggregateId == event.aggregateId
users.userAggregateId == event.aggregateId
```

### Inter-Invariants

```langium
InterInvariant:
    'interInvariant' name=ID '{'
        (subscribedEvents+=SubscribedEvent)*
    '}';
```

## References

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

## Repository

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

### JPQL Query Rules

- Use standard JPQL syntax
- Parameter binding: `:paramName` matches method parameter names
- Return types: entity, collection, `Set<Integer>`, `Optional<T>`

## Service

```langium
ServiceDefinition:
    'Service' (name=ID)? '{'
        (transactional?='@Transactional')?
        ('methods' '{'
            serviceMethods+=ServiceMethod*
        '}')?
    '}';
```

## Web API Endpoints

```langium
WebAPIEndpoints:
    'WebAPIEndpoints' '{'
        (endpoints+=CustomEndpoint)*
    '}';

CustomEndpoint:
    'Endpoint' name=ID '{'
        ('httpMethod' ':' method=HttpMethod)?
        ('path' ':' path=STRING)?
        ('methodName' ':' methodName=ID)?
        ('parameters' ':' '[' parameters+=EndpointParameter (',' parameters+=EndpointParameter)* ']')?
        ('returnType' ':' returnType=ParamType)?
        ('desc' ':' description=STRING)?
    '}';

HttpMethod:
    method=('GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH');
```

## Shared Enums

```langium
SharedEnums:
    'SharedEnums' '{'
        enums+=EnumDefinition*
    '}';

EnumDefinition:
    'enum' name=ID '{'
        values+=EnumValue (',' values+=EnumValue)*
    '}';
```

## Exception Messages

```langium
ExceptionMessages:
    'exceptions' '{'
        messages+=ExceptionMessage*
    '}';

ExceptionMessage:
    name=ID ':' message=STRING;
```

## Annotations

```langium
Annotation:
    '@' name=ID ('(' (values+=AnnotationValue (',' values+=AnnotationValue)*)? ')')?;
```

## Parameters

```langium
Parameter:
    (annotations+=Annotation)*
    type=ParamType name=ID;
```

## Return Types

```langium
ReturnType:
    'void' | PrimitiveType | EntityType | BuiltinType |
    ListType | SetType | OptionalType;
```

## Terminals

### Identifiers

```langium
terminal ID: /[_a-zA-Z][a-zA-Z0-9_]*/;
```

Valid: `User`, `userName`, `_internal`, `user123`

Invalid: `123user`, `user-name`

### Literals

```langium
terminal LITERAL: /'[^']*'/ | /[0-9]+(\.[0-9]+)?/ | 'DELETED' | 'ANONYMOUS';
```

### Strings

```langium
terminal STRING: /"[^"]*"/;
```

### Comments

```nebula
// Single-line comment

/*
 * Multi-line comment
 */
```

## Reserved Keywords

`Aggregate`, `Root`, `Entity`, `Dto`, `from`, `map`, `as`, `invariants`, `check`, `error`, `Repository`, `Query`, `Events`, `publish`, `subscribe`, `routing`, `interInvariant`, `References`, `onDelete`, `prevent`, `cascade`, `setNull`, `Service`, `Transactional`, `methods`, `WebAPIEndpoints`, `Endpoint`, `SharedEnums`, `enum`, `exceptions`, `true`, `false`, `null`

## Complete Example

```nebula
SharedEnums {
    enum UserRole { STUDENT, TEACHER, ADMIN }
    enum CourseType { TECNICO, EXTERNAL }
}

Aggregate User {
    @GenerateCrud

    Root Entity User {
        String name
        String username
        final UserRole role
        Boolean active = true

        invariants {
            check nameNotBlank { name.length() > 0 }
                error "Name cannot be blank"
            check usernameNotBlank { username.length() > 0 }
                error "Username cannot be blank"
            check roleNotNull { role != null }
                error "Role is required"
        }
    }

    Events {
        publish UserDeleted {
            Integer userId
            String username
        }
        publish UserUpdated {
            Integer userId
            String newName
        }
    }

    Repository {
        Optional<User> findByUsername(String username)
        List<User> findByRole(UserRole role)
    }
}

Aggregate Execution {
    @GenerateCrud

    Entity ExecutionCourse from Course {
        map name as courseName
        map type as courseType
    }

    Entity ExecutionUser from User {
        map name as userName
        map username as userUsername
    }

    Root Entity Execution {
        String acronym
        String academicTerm
        LocalDateTime endDate
        ExecutionCourse course
        Set<ExecutionUser> users

        invariants {
            check acronymNotBlank { acronym.length() > 0 }
                error "Acronym cannot be blank"
            check courseNotNull { course != null }
                error "Course is required"
        }
    }

    References {
        course -> Course {
            onDelete: prevent
            message: "Cannot delete course with executions"
        }
    }

    Events {
        interInvariant COURSE_EXISTS {
            subscribe CourseDeletedEvent from Course {
                course.courseAggregateId == event.aggregateId
            }
        }
        interInvariant USERS_EXIST {
            subscribe UserDeletedEvent from User {
                users.userAggregateId == event.aggregateId
            }
        }
    }

    Repository {
        @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
        Set<Integer> findActiveExecutionIds()
    }
}

exceptions {
    USER_NOT_FOUND: "User with ID %d not found"
    EXECUTION_NOT_FOUND: "Execution with ID %d not found"
}
```

---

**Previous:** [04-Generated-Code](04-Generated-Code.md) | **Back to:** [01-Introduction](01-Introduction.md)
