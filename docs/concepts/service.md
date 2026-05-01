# Service Layer

A `*Service` class holds the business logic for one aggregate type. It is the only class allowed to load, mutate, and persist that aggregate. All service methods are called through the aggregate's `CommandHandler` via `commandGateway`.

---

## Injected Dependencies

A service may inject only components that belong to its own aggregate, plus shared infrastructure:

```java
@Service
public class ExecutionService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private CourseExecutionFactory courseExecutionFactory;   // own aggregate's factory

    private final CourseExecutionRepository courseExecutionRepository;            // own aggregate's JPA repo
    private final CourseExecutionCustomRepository courseExecutionCustomRepository; // own aggregate's custom repo
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ExecutionService(UnitOfWorkService unitOfWorkService,
                            CourseExecutionRepository courseExecutionRepository,
                            CourseExecutionCustomRepository courseExecutionCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.courseExecutionRepository = courseExecutionRepository;
        this.courseExecutionCustomRepository = courseExecutionCustomRepository;
    }
}
```

Never inject a foreign service class or a foreign repository — see [R1, R2 in architecture.md](../architecture.md).

> **Inject factories and repositories via their abstract interfaces, not the concrete sagas-profile classes.** The example above injects `CourseExecutionFactory` (the interface defined in `aggregate/`) — `SagasCourseExecutionFactory` is never referenced in the service. This keeps the service layer profile-agnostic and allows a TCC or other implementation to be wired in without touching the service.

---

## Method Patterns

Every service method is annotated `@Transactional(isolation = Isolation.SERIALIZABLE)`. This makes P3 guards race-free.

### Read method

Loads the latest version and converts it to a DTO. No mutation, no `registerChanged`.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public CourseExecutionDto getCourseExecutionById(Integer executionAggregateId, UnitOfWork unitOfWork) {
    return courseExecutionFactory.createCourseExecutionDto(
            (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork));
}
```

### Create method

Generates a new aggregate ID, constructs the aggregate via the factory, registers it as changed.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public CourseExecutionDto createCourseExecution(CourseExecutionDto dto, UnitOfWork unitOfWork) {
    // [P3] uniqueness guard — reads own table, inside @Transactional(SERIALIZABLE)
    Set<Integer> existingIds = courseExecutionCustomRepository.findCourseExecutionIdsOfAllNonDeleted();
    for (Integer id : existingIds) {
        Execution existing = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
        if (existing.getAcronym().equals(dto.getAcronym())
                && existing.getAcademicTerm().equals(dto.getAcademicTerm())) {
            throw new QuizzesException(DUPLICATE_COURSE_EXECUTION, dto.getAcronym(), dto.getAcademicTerm());
        }
    }

    Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
    Execution execution = courseExecutionFactory.createCourseExecution(aggregateId, dto, ...);

    unitOfWorkService.registerChanged(execution, unitOfWork);
    return courseExecutionFactory.createCourseExecutionDto(execution);
}
```

### Mutate method

Loads the current version, copies it via the factory's `create{Aggregate}Copy` method, applies mutations on the copy, then registers the copy as changed.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void enrollStudent(Integer executionAggregateId, UserDto userDto, UnitOfWork unitOfWork) {
    // [P3] guard — validates DTO field assembled by preceding saga step
    Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
            executionAggregateId, unitOfWork);

    if (!userDto.isActive()) {
        throw new QuizzesException(INACTIVE_USER, userDto.getAggregateId());
    }

    // copy-on-write: mutations go on the new version, not the loaded one
    Execution newExecution = courseExecutionFactory.createCourseExecutionCopy(oldExecution);
    newExecution.addStudent(new CourseExecutionStudent(userDto));

    unitOfWorkService.registerChanged(newExecution, unitOfWork);
}
```

### Mutate method with event publication

After `registerChanged`, register the event. The event is persisted atomically when the UoW commits.

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public void removeCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
    Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(
            executionAggregateId, unitOfWork);
    Execution newExecution = courseExecutionFactory.createCourseExecutionCopy(oldExecution);

    newExecution.remove();   // sets state = DELETED

    unitOfWorkService.registerChanged(newExecution, unitOfWork);
    unitOfWorkService.registerEvent(
            new DeleteCourseExecutionEvent(newExecution.getAggregateId()), unitOfWork);
}
```

---

## Copy-on-Write Rule

Never mutate the aggregate instance returned by `aggregateLoadAndRegisterRead`. Always create a new version via `factory.createXxxFromExisting(old)` and mutate that copy. The old version remains in the UoW read set for conflict detection; the new version is the write target.

**Exception — soft-delete (`remove()`):** When a service method calls `aggregate.remove()` to transition the aggregate to a terminal deleted state, mutate the loaded aggregate in-place and register it directly:

```java
Course course = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
course.remove();
unitOfWorkService.registerChanged(course, unitOfWork);
```

No factory copy is needed. `remove()` is a terminal state transition; it does not interact with version-merge conflict detection.

> **Note:** Some reference implementations (e.g., the `quizzes` `UserService`) still create a factory copy before calling `remove()` for defensive consistency. Both are valid. For new aggregates in `quizzes-full`, follow the no-copy pattern shown above — the `CourseService` is the canonical precedent.

---

## DTO Immutability (R7)

A Functionality method receives saga-assembled DTOs from preceding steps. Never mutate a DTO passed into a service method — it is a read-only snapshot from an upstream aggregate. Extract the field values you need and pass them directly to the aggregate or a new value object; do not call any setters on the DTO itself.

---

## P3 Guard Placement

P3 guards (own-table reads, uniqueness checks, and DTO field validation from preceding saga steps) belong at the **top** of the service method, before any `createFromExisting` call. Throwing at this point ensures no aggregate is dirtied before the guard fires. See [`rule-enforcement-patterns.md`](rule-enforcement-patterns.md) for the full taxonomy.

---

## Reference Implementations (Quizzes)

- `applications/quizzes/src/main/java/.../execution/service/ExecutionService.java` — create, read, mutate, and event publication patterns
- `applications/quizzes/src/main/java/.../tournament/service/TournamentService.java` — multi-field mutation and DTO return
