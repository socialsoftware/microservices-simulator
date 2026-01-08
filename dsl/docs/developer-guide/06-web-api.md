# Step 6 · Web API Endpoints

[📚 Guide Index](00-index.md)

> **Goal:** define REST endpoints using `WebAPIEndpoints` and understand the generated controller/functionalities pattern.

[← Back to Step 5](05-services.md) · [Next → Step 7](07-events.md)

---

## 6.1 Automatic CRUD Endpoints

The simplest way to expose your aggregate is with `@GenerateCrud`:

```nebula
Aggregate User {
    Root Entity User {
        String name;
        String username;
        Boolean active;
    }

    WebAPIEndpoints {
        @GenerateCrud;
    }

    Service UserService {
        @GenerateCrud;
    }
}
```

This generates five standard REST endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/users/create` | Create a new user |
| `GET` | `/users/{userAggregateId}` | Get user by ID |
| `PUT` | `/users` | Update user |
| `DELETE` | `/users/{userAggregateId}` | Delete user |
| `GET` | `/users` | Get all users (or search) |

---

## 6.2 Generated Controller Structure

```java
@RestController
public class UserController {
    @Autowired
    private UserFunctionalities userFunctionalities;

    @PostMapping("/users/create")
    public UserDto createUser(@RequestBody UserDto userDto) {
        return userFunctionalities.createUser(userDto);
    }

    @GetMapping("/users/{userAggregateId}")
    public UserDto getUserById(@PathVariable Integer userAggregateId) {
        return userFunctionalities.getUserById(userAggregateId);
    }

    @PutMapping("/users")
    public UserDto updateUser(@RequestBody UserDto userDto) {
        return userFunctionalities.updateUser(userDto);
    }

    @DeleteMapping("/users/{userAggregateId}")
    public void deleteUser(@PathVariable Integer userAggregateId) {
        userFunctionalities.deleteUser(userAggregateId);
    }

    @GetMapping("/users")
    public List<UserDto> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean active) {
        return userFunctionalities.searchUsers(name, username, active);
    }
}
```

---

## 6.3 The Functionalities Layer

Controllers don't call services directly—they go through a **Functionalities** class that handles saga coordination:

```java
@Component
public class UserFunctionalities {
    @Autowired
    private UserService userService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    public UserDto createUser(UserDto userDto) {
        checkInput(userDto);

        // Create saga-managed unit of work
        SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork();

        // Build and execute the saga workflow
        CreateUserFunctionalitySagas saga = new CreateUserFunctionalitySagas(
            userService, sagaUnitOfWorkService, unitOfWork, userDto
        );

        saga.buildWorkflow();
        sagaUnitOfWorkService.executeWorkflow(saga, unitOfWork);

        return saga.getResult();
    }

    private void checkInput(UserDto userDto) {
        if (userDto.getName() == null) {
            throw new AnswersException(USER_MISSING_NAME);
        }
        if (userDto.getUsername() == null) {
            throw new AnswersException(USER_MISSING_USERNAME);
        }
    }
}
```

This separation enables:
- Input validation before saga execution
- Saga workflow orchestration
- Cross-aggregate coordination
- Consistent error handling

---

## 6.4 Searchable Parameters

When CRUD is generated, the `GET /users` endpoint automatically includes searchable parameters based on the entity's String, Boolean, and enum fields:

```nebula
Root Entity User {
    String name;           // → @RequestParam String name
    String username;       // → @RequestParam String username
    Boolean active;        // → @RequestParam Boolean active
    UserRole role;         // → @RequestParam UserRole role (enum)
    LocalDateTime created; // Not searchable (temporal type)
    Integer age;           // Not searchable (numeric type)
}
```

```java
@GetMapping("/users")
public List<UserDto> searchUsers(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) UserRole role) {
    return userFunctionalities.searchUsers(name, username, active, role);
}
```

---

## 6.5 Custom Endpoints

Define custom endpoints alongside CRUD:

```nebula
WebAPIEndpoints {
    @GenerateCrud;

    Endpoint findByExecution {
        httpMethod: GET
        path: "/quizzes/execution/{executionId}"
        methodName: findQuizzesByExecution
        parameters: [
            executionId: Integer: "@PathVariable"
        ]
        returnType: List<QuizDto>
        desc: "Find all quizzes for an execution"
    }

    Endpoint updateDates {
        httpMethod: PUT
        path: "/quizzes/{quizId}/dates"
        methodName: updateQuizDates
        parameters: [
            quizId: Integer: "@PathVariable",
            availableDate: LocalDateTime: "@RequestParam",
            conclusionDate: LocalDateTime: "@RequestParam"
        ]
        returnType: QuizDto
        desc: "Update quiz availability dates"
        throwsException: true
    }
}
```

```java
@GetMapping("/quizzes/execution/{executionId}")
public List<QuizDto> findQuizzesByExecution(@PathVariable Integer executionId) {
    return quizFunctionalities.findQuizzesByExecution(executionId);
}

@PutMapping("/quizzes/{quizId}/dates")
public QuizDto updateQuizDates(
        @PathVariable Integer quizId,
        @RequestParam LocalDateTime availableDate,
        @RequestParam LocalDateTime conclusionDate) throws Exception {
    return quizFunctionalities.updateQuizDates(quizId, availableDate, conclusionDate);
}
```

---

## 6.6 Parameter Annotations

| Annotation | Use Case | Example |
|------------|----------|---------|
| `@PathVariable` | URL path segments | `/users/{userId}` |
| `@RequestParam` | Query parameters | `/users?active=true` |
| `@RequestBody` | JSON request body | POST/PUT payloads |
| `@RequestParam(required = false)` | Optional query params | Search filters |

```nebula
parameters: [
    userId: Integer: "@PathVariable",
    name: String: "@RequestParam",
    userDto: UserDto: "@RequestBody",
    active: Boolean: "@RequestParam(required = false)"
]
```

---

## 6.7 Return Types

Endpoints can return:

```nebula
// Single DTO
returnType: UserDto

// Collection of DTOs
returnType: List<UserDto>
returnType: Set<UserDto>

// Void (no response body)
// Simply omit the returnType field

// Primitives
returnType: Integer
returnType: Boolean
```

---

## 6.8 Exception Handling

Mark endpoints that can throw exceptions:

```nebula
Endpoint createQuiz {
    httpMethod: POST
    path: "/quizzes/create"
    methodName: createQuiz
    parameters: [
        quizDto: QuizDto: "@RequestBody"
    ]
    returnType: QuizDto
    throwsException: true
}
```

```java
@PostMapping("/quizzes/create")
public QuizDto createQuiz(@RequestBody QuizDto quizDto) throws Exception {
    return quizFunctionalities.createQuiz(quizDto);
}
```

The global exception handler converts domain exceptions to appropriate HTTP responses.

---

## 6.9 Complete Example

```nebula
Aggregate Quiz {
    Root Entity Quiz {
        String title;
        QuizType quizType;
        LocalDateTime availableDate;
        LocalDateTime conclusionDate;
        QuizExecution execution;
    }

    WebAPIEndpoints {
        @GenerateCrud;

        Endpoint findByExecution {
            httpMethod: GET
            path: "/quizzes/execution/{executionId}"
            methodName: findQuizzesByExecution
            parameters: [
                executionId: Integer: "@PathVariable"
            ]
            returnType: List<QuizDto>
            desc: "Find quizzes by execution"
        }

        Endpoint publish {
            httpMethod: POST
            path: "/quizzes/{quizId}/publish"
            methodName: publishQuiz
            parameters: [
                quizId: Integer: "@PathVariable"
            ]
            returnType: QuizDto
            desc: "Publish a quiz"
            throwsException: true
        }
    }

    Service QuizService {
        @GenerateCrud;
    }
}
```

---

## Recap

The `WebAPIEndpoints` block generates REST controllers that delegate to the functionalities layer. The functionalities layer orchestrates saga workflows and calls services. Use `@GenerateCrud` for standard operations and custom `Endpoint` definitions for domain-specific operations.

[← Back to Step 5](05-services.md) · [Next → Step 7](07-events.md)
