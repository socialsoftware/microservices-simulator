# Test Analysis Saga Input Flow

## Purpose

This note explains how the verifier currently turns Quizzes/Groovy test code into scenario-catalog `InputVariant`s, and what the smoke-test results imply for future executor work.

## Current Flow

### 1. Java analysis discovers saga definitions

For a saga class such as:

```java
public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
  public CreateTournamentFunctionalitySagas(
      SagaUnitOfWorkService sagaUnitOfWorkService,
      Integer userId,
      Integer courseExecutionId,
      Set<Integer> topicIds,
      TournamentDto tournamentDto,
      SagaUnitOfWork sagaUnitOfWork,
      CommandGateway commandGateway
  ) { ... }
}
```

the Java visitors record:

- the saga FQN;
- constructor argument positions and expected types;
- ordered saga steps;
- per-step dispatch footprints and aggregate/access information where statically available.

This gives the analyzer the target constructor shape that later test traces must satisfy.

### 2. Java analysis discovers functionality creation sites

Quizzes tests often call façade methods instead of directly constructing saga classes:

```groovy
tournamentFunctionalities.createTournament(userId, courseExecutionId, topicIds, tournamentDto)
```

The corresponding Java functionality method usually contains a branch similar to:

```java
switch (workflowType) {
  case SAGAS:
    var uow = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
    var saga = new CreateTournamentFunctionalitySagas(
        sagaUnitOfWorkService, userId, courseExecutionId, topicIds, tournamentDto, uow, commandGateway);
    saga.executeWorkflow(uow);
    return saga.getCreatedTournamentDto();
  case TCC:
    var uow = causalUnitOfWorkService.createUnitOfWork(functionalityName);
    var tcc = new CreateTournamentFunctionalityTCC(...);
    tcc.executeWorkflow(uow);
    return tcc.getCreatedTournamentDto();
}
```

The creation-site visitor records that the façade method can create a saga and maps façade arguments to saga constructor arguments. For the saga branch, this yields a mapping like:

```text
CreateTournamentFunctionalitySagas(
  sagaUnitOfWorkService,  // injectable field
  userId,                 // façade arg 0
  courseExecutionId,      // façade arg 1
  topicIds,               // façade arg 2
  tournamentDto,          // façade arg 3
  sagaUnitOfWork,         // runtime-created unit of work
  commandGateway          // injectable field
)
```

The verifier currently records saga creation sites, but it does not yet fully classify the source test's runtime mode when the same façade type can execute either SAGAS or TCC depending on Spring profile/configuration.

### 3. Groovy source indexing parses tests

The Groovy source index parses Spock test files and records fields, setup methods, local assignments, method calls, and test methods.

Example:

```groovy
def setup() {
  courseExecutionDto = createCourseExecution(...)
  userCreatorDto = createUser(...)
  topicDto = createTopic(courseExecutionDto, ...)
}

def "create tournament successfully"() {
  tournamentFunctionalities.createTournament(
      userCreatorDto.aggregateId,
      courseExecutionDto.aggregateId,
      [topicDto.aggregateId],
      tournamentDto)
}
```

### 4. Groovy tracing identifies saga invocation patterns

There are two main patterns.

#### Direct saga construction

```groovy
def saga = new CreateTournamentFunctionalitySagas(...)
```

The target saga is explicit, and each constructor argument is traced directly.

#### Functionality façade call

```groovy
tournamentFunctionalities.createTournament(...)
```

The visitor checks whether Java analysis found a creation site for this façade method. If so, it maps Groovy call arguments through the Java creation-site mapping into the target saga constructor arguments.

### 5. Each constructor argument is traced to its origin

The tracer follows local variables, fields, simple property/method access, DTO construction, helper calls, and runtime-producing calls where possible.

Typical classifications:

- `RESOLVED`: statically known literal or DTO construction detail.
- `INJECTABLE_PLACEHOLDER`: expected infrastructure such as `sagaUnitOfWorkService` or `commandGateway`.
- `RUNTIME_CALL` / replayable edge: value produced by calling application functionality or unit-of-work setup.
- `PARTIAL`: a useful trace exists but some required part cannot be fully reconstructed.
- `UNRESOLVED`: insufficient information to create even a replay recipe.

For example:

```text
arg[1]: courseExecutionDto
  <- createCourseExecution(...)
  <- courseExecutionFunctionalities.createCourseExecution(courseExecutionDto)
  [runtime edge].aggregateId
```

This does not mean the verifier knows the concrete ID. It means a future executor may need to replay the setup call and then read the resulting DTO/aggregate ID.

### 6. The trace is classified by source mode

Before scenario adaptation/filtering, Groovy traces carry source-mode metadata inferred from generic Spring/simulator evidence:

- autowired `SagaUnitOfWorkService` / `CausalUnitOfWorkService` fields;
- local/nested `@TestConfiguration` bean evidence for those services;
- explicit `@ActiveProfiles`, `@TestPropertySource`, and `@SpringBootTest(properties=...)` profile evidence for `sagas` / `tcc`.

The classifier emits:

```text
sourceMode: SAGAS | TCC | MIXED | UNKNOWN
sourceModeConfidence: TEST_CONFIGURATION | TYPE_EVIDENCE | ACTIVE_PROFILE | UNKNOWN
sourceModeEvidence: [human-readable evidence strings]
```

Conflicting active-profile and test-configuration evidence is classified as `MIXED`. Unsupported aliases/placeholders are not guessed.

### 7. The trace becomes an `InputVariant`

The scenario adapter converts each usable full trace into an `InputVariant` with:

- target saga FQN;
- source test class and method;
- source expression such as `tournamentFunctionalities.createTournament(...)`;
- source-mode metadata;
- constructor argument summaries;
- replay status such as `REPLAYABLE` or `PARTIAL`;
- warnings/diagnostics;
- logical key bindings, currently empty for Quizzes smoke output.

Current catalog inputs are therefore provenance-rich replay instructions, not fully materialized concrete JSON values.

### 8. Source-mode policy filters saga-catalog inputs

The scenario generator applies the saga-catalog source-mode policy before accepted-input deduplication and scenario planning:

```text
SAGAS   => accept
TCC     => reject into scenario-catalog-rejected-inputs.jsonl
MIXED   => reject into scenario-catalog-rejected-inputs.jsonl
UNKNOWN => accept with a warning
```

Rejected records retain deterministic IDs, source location, source-mode evidence, warnings, and rejection reasons. The rejected-input JSONL file is written whenever catalog export is enabled, even if empty.

### 9. Single-saga scenario generation

For each accepted input variant, the generator emits a single-saga `ScenarioPlan`:

```text
SagaDefinition + InputVariant -> SINGLE_SAGA scenario
```

The plan contains one saga instance, the selected input variant, an expanded step schedule, and a binary fault slot per scheduled step.

## Quizzes Single-Saga Smoke Observations

With `maxSagaSetSize=1`, high caps, and `inputPolicy=RESOLVED_OR_REPLAYABLE`, the Quizzes smoke produced:

```text
sagaBlocksSeen: 65
sagasAdapted: 65
inputTracesSeen: 1581
inputVariantsAdapted: 549
inputVariantsDeduplicated: 1032
inputVariantsAccepted: 537
inputVariantsExcludedByPolicy: 12
inputSagasWithInputs: 25
sagasWithoutUsableInputs: 40
scenariosExported: 537
```

Allowing partial inputs increased the catalog to 549 scenarios, meaning the strict default excludes only 12 partial variants and does not add new saga classes.

The smoke shows all expected aggregate footprint types, including Course, Execution, Question, Quiz, QuizAnswer, Topic, Tournament, and User. However, only 25 of 65 saga classes currently have accepted input variants. Question saga classes exist but do not currently get standalone input variants from the observed tests; many question operations appear as helper/setup calls used while preparing other scenarios.

## Design Implications

### Exhaustive functionality calls

For scenario generation, it is preferable to treat every valid functionality call as a candidate input, including calls nested inside setup/helper methods. Such candidates should be labelled by role rather than discarded:

- primary test subject;
- setup call;
- helper-derived call.

This keeps the catalog exhaustive while allowing downstream configuration to select which roles to execute.

### Runtime mode classification

Quizzes has both SAGAS and TCC/causal configurations. The same façade class, such as `TournamentFunctionalities`, can branch internally on active transactional model/profile. Source calls now carry source-mode metadata, but the implementation remains intentionally evidence-based rather than a full Spring environment solver.

The generic mechanism avoids hardcoding Quizzes names. It currently infers mode from Spring/simulator evidence such as unit-of-work service fields, test configuration bean types, and explicit profile properties. Package/class-name hints are not primary evidence. Branch selection inside a façade remains a separate Java-side fact; TCC runtime execution/generation is still out of scope for the saga catalog.

### Replay recipes vs concrete values

The future executor should not expect all input values to be statically materialized. Many important values are runtime products of setup calls, such as aggregate IDs returned after creating users, topics, course executions, questions, or tournaments.

The current catalog therefore should be viewed as carrying replay-oriented input instructions. Future work should make those recipes more structured so the executor can:

1. run setup functionality calls in order;
2. retain returned DTOs/aggregate IDs;
3. bind those runtime values into the target saga constructor inputs;
4. execute the target saga step by step.

## Current Gaps To Defer

- Promote helper-internal functionality calls into standalone input variants.
- Extend source-mode classification only if more generic Spring evidence is needed; avoid Quizzes-specific shortcuts.
- Extract logical aggregate key bindings from traced arguments.
- Convert textual replay provenance into structured executor-ready setup recipes.
- Decide whether future TCC-specific catalogs/executors should consume TCC-derived input variants instead of rejecting them from the saga catalog.
