# Grammar Reference

Complete syntax reference for the Nebula DSL. This chapter documents all grammar rules with examples.

## Model (Entry Point)

The top-level structure of a `.nebula` file.

```langium
entry Model:
    (aggregates+=Aggregate | sharedEnums+=SharedEnums)*
    (exceptions=ExceptionMessages)?;
```

**Example:**
```nebula
SharedEnums {
    enum UserRole { STUDENT, TEACHER, ADMIN }
}

Aggregate User { ... }
Aggregate Course { ... }

exceptions {
    USER_NOT_FOUND: "User with ID %d not found"
}
```

## Aggregate

Defines a bounded context containing entities, events, and business logic.

```langium
Aggregate:
    (annotations+=Annotation)*
    'Aggregate' name=ID '{'
        (generateCrud?='@GenerateCrud')?
        (aggregateElements+=AggregateElement)*
    '}';
```

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

## Aggregate Elements

Elements that can appear inside an aggregate.

```langium
AggregateElement:
    Entity | Method | Workflow | Repository | Events |
    References | WebAPIEndpoints | ServiceDefinition | Functionalities;
```

### Cardinality Rules

| Element | Allowed Count |
|---------|---------------|
| Entity | 0 or more |
| Method | 0 or more |
| Workflow | 0 or more |
| Repository | 0 or 1 |
| Events | 0 or 1 |
| References | 0 or 1 |
| WebAPIEndpoints | 0 or 1 |
| ServiceDefinition | 0 or 1 |
| Functionalities | 0 or 1 |

## Entity

Defines a domain object (root or child entity).

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

**Root Entity:**
```nebula
Root Entity User {
    String name
    String username
    UserRole role
}
```

**Child Entity:**
```nebula
Entity Address {
    String street
    String city
    String zipCode
}
```

**Cross-Aggregate Entity:**
```nebula
Entity ExecutionCourse from Course {
    map name as courseName
    map type as courseType
}
```

**Empty Entity (marker):**
```nebula
Dto Entity UserDto
```

## Property

Defines an entity field.

```langium
Property:
    (isFinal?='final')? type=Type name=ID (',' names+=ID)*
    ('=' defaultValue=PropertyDefaultValue)?
    (dtoExclude?='dto-exclude')?;
```

**Examples:**
```nebula
String name
final UserRole role
Boolean active = true
Integer count = 0
AggregateState state = AggregateState.ACTIVE
String internalId dto-exclude
String firstName, lastName, middleName
```

### Default Values

```langium
PropertyDefaultValue returns string:
    LITERAL
  | STRING
  | QualifiedName
  | AggregateStateQualified
  | 'true'
  | 'false'
  | 'null'
  | '[]';
```

**Examples:**
```nebula
Boolean active = true
Integer count = 0
String status = "pending"
AggregateState state = AggregateState.ACTIVE
List<Integer> scores = []
```

## Field Mapping

Maps fields from cross-aggregate references.

```langium
DtoFieldMapping:
    'map' dtoField=DtoFieldPath 'as' entityField=ID;

DtoFieldPath:
    parts+=ID ('.' parts+=ID)*;
```

**Simple mapping:**
```nebula
map name as courseName
map type as courseType
```

**Extract pattern (future):**
```nebula
map questions.aggregateId as questionIds
```

## Type System

### Type

```langium
Type:
    BaseType | ListType | SetType | OptionalType;

BaseType:
    PrimitiveType | BuiltinType | EntityType;
```

### Primitive Types

```langium
PrimitiveType:
    typeName=('Integer' | 'Long' | 'Float' | 'Double' |
              'String' | 'Boolean' | 'LocalDateTime' | 'Object');
```

**Examples:**
```nebula
String name
Integer age
Long timestamp
Float price
Double percentage
Boolean active
LocalDateTime createdAt
```

### Builtin Types

```langium
BuiltinType:
    typeName=('UnitOfWork' | 'AggregateState');
```

**Examples:**
```nebula
UnitOfWork unitOfWork
AggregateState state
```

### Entity Type

```langium
EntityType:
    type=[Entity:QualifiedName];
```

**Examples:**
```nebula
UserRole role
ExecutionCourse course
```

### Collection Types

```langium
ListType:
    'List' '<' elementType=BaseType '>';

SetType:
    'Set' '<' elementType=BaseType '>';

OptionalType:
    'Optional' '<' elementType=BaseType '>';
```

**Examples:**
```nebula
List<Integer> scores
Set<ExecutionUser> users
Optional<LocalDateTime> deadline
```

**Constraints:**
- No nested collections: `List<List<Integer>>` ❌
- No void in collections: `Set<void>` ❌
- Element type must be BaseType

## Invariants

Business rules enforced at aggregate level.

```langium
Invariant:
    'check' name=ID '{'
        conditions+=InvariantCondition*
    '}'
    'error' errorMessage=STRING;

InvariantCondition:
    expression=Expression;
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

### Expressions

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

**Operators:**
- Boolean: `&&`, `||`, `!`, `AND`, `OR`, `NOT`
- Comparison: `<`, `>`, `<=`, `>=`, `==`, `!=`
- Arithmetic: `+`, `-`, `*`, `/`, `%`

**Examples:**
```nebula
age >= 18
count < maxCount
active && verified
role == ADMIN || role == TEACHER
!deleted
price * quantity > 100
```

### Method Expressions

```langium
MethodExpression:
    UniqueCheckExpression |
    CollectionOperationExpression |
    TimeExpression |
    QuantifierExpression;
```

**Unique check:**
```nebula
topics.unique(topicId)
```

**Collection operations:**
```nebula
users.size()
users.isEmpty()
questions.count()
```

**Time operations:**
```nebula
startDate.isBefore(endDate)
deadline.isAfter(now())
createdAt.isEqual(updatedAt)
```

**Quantifiers (experimental):**
```nebula
forall u : users | u.active == true
exists t : topics | t.name == "Math"
```

## Events

Pub/sub event system.

```langium
Events:
    'Events' '{'
        (publishedEvents+=PublishedEvent)*
        (subscribedEvents+=SubscribedEvent)*
        (interInvariants+=InterInvariant)*
    '}';
```

### Published Events

```langium
PublishedEvent:
    'publish' name=ID '{'
        fields+=EventField*
    '}';

EventField:
    type=Type name=ID;
```

**Example:**
```nebula
Events {
    publish UserDeleted {
        Integer userId
        String username
        UserRole role
    }

    publish UserUpdated {
        Integer userId
        String newName
    }
}
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

**Simple subscription:**
```nebula
subscribe CourseDeletedEvent
```

**With source:**
```nebula
subscribe UserDeletedEvent from User
```

**With routing:**
```nebula
subscribe UserUpdatedEvent from User routing (users.userAggregateId)
```

**With condition:**
```nebula
subscribe CourseDeletedEvent from Course {
    course.courseAggregateId == event.aggregateId
}
```

### Inter-Invariants

```langium
InterInvariant:
    'interInvariant' name=ID '{'
        (subscribedEvents+=SubscribedEvent)*
    '}';
```

**Example:**
```nebula
Events {
    interInvariant USERS_EXIST {
        subscribe UserDeletedEvent from User {
            users.userAggregateId == event.aggregateId
        }
        subscribe UserUpdatedEvent from User {
            users.userAggregateId == event.aggregateId
        }
    }
}
```

## References

Referential integrity constraints.

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

## Repository

Custom database queries.

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

**Spring Data methods:**
```nebula
Repository {
    Optional<User> findByUsername(String username)
    List<User> findByRole(UserRole role)
}
```

**Custom JPQL queries:**
```nebula
Repository {
    @Query("select e.aggregateId from Execution e where e.state != 'DELETED'")
    Set<Integer> findActiveExecutionIds()

    @Query("select a from Answer a where a.quiz.quizAggregateId = :quizId")
    List<Answer> findByQuizId(Integer quizId)
}
```

## Service

Service layer definition.

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

**Auto-named service:**
```nebula
Service {
    @Transactional
}
// Generated: UserService (from aggregate name)
```

**Named service with methods:**
```nebula
Service UserService {
    @Transactional

    methods {
        activateUser(Integer userId, UnitOfWork unitOfWork): UserDto
        deactivateUser(Integer userId, UnitOfWork unitOfWork): UserDto
    }
}
```

## Web API Endpoints

REST API definition.

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
        ('throwsException' ':' throwsException=('true' | 'false'))?
    '}';

HttpMethod:
    method=('GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH');
```

**Example:**
```nebula
WebAPIEndpoints {
    Endpoint activateUser {
        httpMethod: POST
        path: "/users/{id}/activate"
        methodName: activateUser
        parameters: [id: Integer: "@PathVariable"]
        returnType: UserDto
        desc: "Activate a user account"
    }
}
```

## Shared Enums

Enumerations shared across aggregates.

```langium
SharedEnums:
    'SharedEnums' '{'
        enums+=EnumDefinition*
    '}';

EnumDefinition:
    'enum' name=ID '{'
        values+=EnumValue (',' values+=EnumValue)*
    '}';

EnumValue:
    name=ID;
```

**Example:**
```nebula
SharedEnums {
    enum UserRole {
        STUDENT, TEACHER, ADMIN
    }

    enum CourseType {
        TECNICO, EXTERNAL
    }

    enum QuestionType {
        MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER
    }
}
```

## Exception Messages

Custom error messages.

```langium
ExceptionMessages:
    'exceptions' '{'
        messages+=ExceptionMessage*
    '}';

ExceptionMessage:
    name=ID ':' message=STRING;
```

**Example:**
```nebula
exceptions {
    USER_NOT_FOUND: "User with ID %d not found"
    INVALID_USERNAME: "Username '%s' is invalid"
    DUPLICATE_EMAIL: "Email '%s' is already registered"
}
```

## Annotations

Metadata markers.

```langium
Annotation:
    '@' name=ID ('(' (values+=AnnotationValue (',' values+=AnnotationValue)*)? ')')?;

AnnotationValue:
    key=ID '=' value=(STRING | ID | LITERAL) |
    value=(STRING | ID | LITERAL);
```

**Examples:**
```nebula
@GenerateCrud
@Transactional
@Query("select * from users")
@Cacheable(value = "users", key = "#id")
```

## Parameters

Method parameters.

```langium
Parameter:
    (annotations+=Annotation)*
    type=ParamType name=ID;
```

**Examples:**
```nebula
Integer id
UserDto dto
@PathVariable Integer userId
@RequestBody CreateUserRequestDto request
```

## Return Types

```langium
ReturnType:
    'void' | PrimitiveType | EntityType | BuiltinType |
    ListType | SetType | OptionalType;
```

**Examples:**
```nebula
void
UserDto
List<UserDto>
Set<Integer>
Optional<User>
```

## Terminals

### ID (Identifier)

```langium
terminal ID:
    /[_a-zA-Z][a-zA-Z0-9_]*/;
```

**Valid:**
- `User`
- `userName`
- `_internal`
- `user123`

**Invalid:**
- `123user` (starts with number)
- `user-name` (contains hyphen)

### LITERAL

```langium
terminal LITERAL:
    /'[^']*'/ | /[0-9]+(\.[0-9]+)?/ | 'DELETED' | 'ANONYMOUS';
```

**Examples:**
- `'value'`
- `123`
- `45.67`
- `DELETED`
- `ANONYMOUS`

### STRING

```langium
terminal STRING:
    /"[^"]*"/;
```

**Examples:**
- `"User not found"`
- `"Error: %s"`

### Comments

```langium
hidden terminal ML_COMMENT:
    /\/\*([^*]|\*+[^*/])*\*+\//;

hidden terminal SL_COMMENT:
    /\/\/[^\n\r]*/;
```

**Examples:**
```nebula
// Single-line comment

/*
 * Multi-line comment
 */

Root Entity User {
    String name  // Field comment
}
```

## Reserved Keywords

Keywords that cannot be used as identifiers:

- `Aggregate`
- `Root`
- `Entity`
- `Dto`
- `from`
- `map`
- `as`
- `invariants`
- `check`
- `error`
- `Repository`
- `Query`
- `Events`
- `publish`
- `subscribe`
- `routing`
- `interInvariant`
- `References`
- `onDelete`
- `prevent`
- `cascade`
- `setNull`
- `Service`
- `Transactional`
- `methods`
- `WebAPIEndpoints`
- `Endpoint`
- `SharedEnums`
- `enum`
- `exceptions`
- `true`
- `false`
- `null`

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

## Next Steps

- **[00-Introduction](00-Introduction.md)** - Start from the beginning
- **[04-DSL-Features](04-DSL-Features.md)** - Feature deep dive
- **[10-Adding-DSL-Features](10-Adding-DSL-Features.md)** - Extend the DSL

---

**Previous:** [10-Adding-DSL-Features](10-Adding-DSL-Features.md) | **Back to:** [00-Introduction](00-Introduction.md)
