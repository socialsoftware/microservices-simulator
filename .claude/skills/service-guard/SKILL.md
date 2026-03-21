---
name: service-guard
description: Add a Layer 3 service-layer guard to an existing service. The guard reads the DB and throws before any mutation. Arguments: "<ServiceName> <operation-method> <precondition-description>"
argument-hint: "<ServiceName> <operation-method> <precondition>"
---

# Add Service-Layer Guard: $ARGUMENTS

You are adding a new **Layer 3 service-layer guard** to an existing service in `applications/quizzes`.

A service-layer guard is a precondition check that reads from the database and throws an exception before any aggregate mutation is applied. It runs inside the same UoW transaction as the operation.

> The guard must only read the service's **own aggregate type**. If a foreign aggregate must be read to evaluate the precondition, use a Layer 5 saga step instead (`/inter-invariant` or a named step in the functionality). Consult `docs/concepts/invariants.md` if uncertain.

---

## Step 0 — Parse the arguments

From `$ARGUMENTS` identify:
- **ServiceName**: the service class to modify (e.g. `ExecutionService`, `TournamentService`)
- **Operation method**: the service method receiving the guard (e.g. `enrollStudent`, `createCourseExecution`)
- **Precondition**: what must hold before the mutation is allowed (e.g. "user must be active", "no duplicate acronym/term")

---

## Step 1 — Read existing code

Before writing anything, read:
1. The service class `microservices/<aggregate>/service/<Service>.java` — focus on the target operation method and how other guards in the same class are structured
2. The custom repository interface `microservices/<aggregate>/aggregate/<Aggregate>CustomRepository.java` (if it exists) and its implementations:
   - `microservices/<aggregate>/aggregate/sagas/<Aggregate>SagaRepository.java`
   - `microservices/<aggregate>/aggregate/causal/<Aggregate>CausalRepository.java`
3. `QuizzesErrorMessage.java` — existing error constants
4. The closest test class under `src/test/groovy/.../sagas/coordination/<aggregate>/`

---

## Step 2 — Design the guard

Decide:
- **Guard placement**: immediately before the copy-constructor call or `registerChanged()`, never after a mutation has been applied
- **Repository query needed**: can the check be done on the aggregate already loaded by the operation, or does it need a separate query (e.g. `findAllAggregateIds()`, a custom finder)?
  - Prefer reusing an already-loaded aggregate over an extra DB read
  - If a new query is needed, add it to the custom repository interface and both implementations (Sagas + TCC)
- **Error constant name**: follow the convention:
  - `CANNOT_<OP>_WHEN_<CONDITION>` (e.g. `CANNOT_ENROLL_WHEN_INACTIVE`)
  - `DUPLICATE_<ENTITY>` (e.g. `DUPLICATE_COURSE_EXECUTION`)
  - `INACTIVE_<ENTITY>` (e.g. `INACTIVE_USER`)
  - Reuse an existing constant if one already expresses the same rule

Document the decision in a brief comment before writing code.

---

## Step 3 — Add the guard check

Inside the service method, add the guard **before** any aggregate mutation, with an inline label comment:

```java
// GUARD_NAME — <one-line description of what is being checked>
if (<precondition violated>) {
    throw new QuizzesException(<ERROR_CONSTANT>, <relevant ids...>);
}
```

Real examples from `ExecutionService.java`:

```java
// DUPLICATE_COURSE_EXECUTION — reject creation if acronym+term already exists
if (existing.getAcronym().equals(courseExecutionDto.getAcronym())
        && existing.getAcademicTerm().equals(courseExecutionDto.getAcademicTerm())) {
    throw new QuizzesException(DUPLICATE_COURSE_EXECUTION,
            courseExecutionDto.getAcronym(), courseExecutionDto.getAcademicTerm());
}

// INACTIVE_USER — block enrollment of inactive users
if (!courseExecutionStudent.isActive()) {
    throw new QuizzesException(INACTIVE_USER, courseExecutionStudent.getUserAggregateId());
}
```

---

## Step 4 — Add repository query (if needed)

If the guard requires a DB query not already available:

1. Add the method signature to the custom repository interface:
   ```java
   // <Aggregate>CustomRepository.java
   List<Integer> findAggregateIdsByXxx(Integer someId);
   ```

2. Implement it in both the Sagas and TCC repository classes with the same logic.

If the guard can be evaluated on an aggregate already loaded by the operation, skip this step.

---

## Step 5 — Add error message constant

In `QuizzesErrorMessage.java`, add the new constant if it does not already exist:

```java
CANNOT_<OP>_WHEN_<CONDITION>("Cannot <operation>: <human-readable reason>"),
```

---

## Step 6 — Write tests

Add test cases to the relevant `*Test.groovy` under `src/test/groovy/.../sagas/coordination/<aggregate>/`.

Cover at minimum:

| Scenario | Expected outcome |
|----------|-----------------|
| Precondition satisfied | Operation succeeds |
| Precondition violated | `QuizzesException` with the correct error code |
| Boundary / edge cases | Correct behaviour |

Run with:
```bash
mvn clean -Ptest-sagas test -Dtest=<YourTestClass>
```

---

## Checklist before finishing

- [ ] Guard check added **before** any mutation in the service method, with inline label comment
- [ ] Repository query added to custom repository interface + both implementations (Sagas + TCC), if a new query was needed
- [ ] Error message constant added to `QuizzesErrorMessage.java` (or existing one reused)
- [ ] Tests written and passing
