---
name: review-aggregate
description: Deep review and comparison of one aggregate implementation in quizzes-full against the quizzes reference app. Reads all relevant files, checks structural correctness, functionality completeness, rule enforcement, test coverage, and build results. Writes a structured review to applications/{app-name}/reviews/review-{Aggregate}.md. Invoke with /review-aggregate <AggregateName> (e.g., /review-aggregate Course).
argument-hint: "<AggregateName> (e.g. Course, CourseExecution, Tournament)"
---

# Review Aggregate

Deep, file-by-file comparison of one aggregate implementation in `quizzes-full` against the `quizzes`
reference app and `plan.md` specification. Produces a structured review report and a prioritised
action-item list.

One aggregate per invocation. Reads files directly from disk — does not synthesise from conversation
context.

---

## Step 1: Resolve Context

### 1.a — Locate plan.md

Run: `find applications -name plan.md`

Use the first result. Extract:
- `{app-name}` = directory containing plan.md (e.g., `quizzes-full`)
- `{pkg}` = `{app-name}` with hyphens removed, lowercase (e.g., `quizzesfull`)
- `{AppClass}` = PascalCase of `{app-name}` (e.g., `QuizzesFull`)

If no plan.md found, halt: "No plan.md found. Run /classify-and-plan first."

### 1.b — Resolve the aggregate

`{Aggregate}` = PascalCase argument (e.g., `Course`).
`{aggregate}` = lowercase (e.g., `course`).

Verify that a section `### N. {Aggregate}` exists in plan.md. If not found, halt:
"Aggregate '{Aggregate}' not found in plan.md. Check the name or run /classify-and-plan."

Extract `{N}` = the ordinal from the section header.

### 1.c — Derive path prefixes

```
{ref-src}     = applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/
{ref-test}    = applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/
{tgt-src}     = applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{tgt-test}    = applications/{app-name}/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/{pkg}/
{review-dir}  = applications/{app-name}/reviews/
{review-file} = {review-dir}review-{Aggregate}.md
```

---

## Step 2: Read Reference Files

Read each file and hold it in context. Absent files are findings, not errors — do not skip them.
Read as many files in parallel as possible.

### 2.a — plan.md aggregate section

Read the entire `### {N}. {Aggregate}` section from plan.md. Extract and hold:
- Write and read functionalities list
- Events published / subscribed
- Cross-aggregate prerequisites (P4a/P4b rules)
- Files-to-produce table (sessions 2.N.a through 2.N.d)
- Checklist — which sessions are ticked
- Any P1/P2/P3 rule notes, including contradiction flags

### 2.b — plan.md Rule Classification tables

Read the `## Rule Classification` section (§3.1 and §3.2 tables). Filter rows relevant to this aggregate:
- P1 rules on this aggregate's entity
- P2 rules where this aggregate is the consumer
- P3 rules where this aggregate's service is the guard location
- P4a/P4b rules where this aggregate's sagas perform data-assembly

### 2.c — Retros for this aggregate

Run: `find applications/{app-name}/retros -name "retro-2.{N}.*-{Aggregate}.md" | sort`

Read every matching retro file. Record for each:
- Action items (Priority + Target + Action)
- One-Line Summary

These are known issues — the review must check whether each source-file-targeted item was resolved.

### 2.d — Reference app files for this aggregate

Enumerate all files under `{ref-src}microservices/{aggregate}/` recursively.
Also: `find applications/quizzes/src/main/java -path "*/commands/{aggregate}/*" -name "*.java"`
Also: `find {ref-test} -path "*/{aggregate}/*" -name "*.groovy"`

Read each file. Build the reference file list.

### 2.e — Target app files for this aggregate

Enumerate all files under `{tgt-src}microservices/{aggregate}/` recursively.
Also: `find applications/{app-name}/src/main/java -path "*/commands/{aggregate}/*" -name "*.java"`
Also: `find {tgt-test} -path "*/{aggregate}*" -name "*.groovy"` (covers `sagas/{aggregate}/` and `sagas/coordination/{aggregate}/`)

Read each file. Build the target file list.

### 2.f — Concept docs

Read sections relevant to this aggregate type:
- `docs/concepts/aggregate.md` — base class, SagaAggregate, verifyInvariants, factories, repositories
- `docs/concepts/service.md` — method patterns, copy-on-write, delete exception, P3 guard placement
- `docs/concepts/commands.md` — command structure, ServiceMapping, CommandHandler, `getAggregateTypeName()`
- `docs/concepts/sagas.md` — SagaWorkflow, SagaStep, lock-acquisition, SagaCommand wrapping
- `docs/concepts/testing.md` — T1/T2/T3 structures, test locations, not-found assertions
- `docs/concepts/events.md` — only if this aggregate has subscribed events (session 2.N.d)

---

## Step 3: File Inventory

Compare the reference file list (Step 2.d) against the target file list (Step 2.e).

For each reference file, determine whether an equivalent target file exists. Match by purpose and naming
convention, not exact path (`{pkg}` replaces `quizzes`, `{AppClass}` replaces `Quizzes`).

| File (relative to microservices/{aggregate}/) | In Reference | In Target | Status | Notes |
|----------------------------------------------|-------------|-----------|--------|-------|

Status values: `OK` / `Missing` / `Extra` / `Intentional` / `Renamed`

**Always Intentional:** `causal/` subtree files — quizzes-full is sagas-only; the TCC variant is not expected.

For target files not in the reference, mark `Extra` with a justification note.

---

## Step 4: Structural Review

For each file present in the target, compare its structure against the reference and concept docs.
For every finding, provide: the file, the expected pattern (with source), the actual finding (quoted
lines), and a status.

Status values: `Correct` / `Minor deviation` / `Incorrect` / `Pattern missing`

### `aggregate/{Aggregate}.java`
- Extends `Aggregate` (not another base class)
- Has `@Entity` annotation
- P1 final-field rules: each such field is declared `final` in Java
- `verifyInvariants()` throws `{AppClass}Exception` for each P1 intra-invariant
- `getEventSubscriptions()` returns empty set if no subscribed events; builds from snapshot fields if P2 inter-invariants exist
- Copy constructor copies all fields including any cached snapshot fields

### `aggregate/sagas/Saga{Aggregate}.java`
- Extends `{Aggregate}` and implements `SagaAggregate`
- `sagaState` field typed as `SagaState` (the interface), NOT the concrete enum
- Default constructor: `sagaState = GenericSagaState.NOT_IN_SAGA`
- Copy constructor: resets `sagaState = GenericSagaState.NOT_IN_SAGA` (does NOT copy `other.getSagaState()`)
  - Note: the quizzes reference copies `other.getSagaState()` — quizzes-full resets to NOT_IN_SAGA. Either is valid; note which is used.

### `aggregate/sagas/states/{Aggregate}SagaState.java`
- Implements `SagaAggregate.SagaState`
- Contains `IN_{OPERATION}` states only for write ops that lock an existing instance (not for create-new)
- Contains `READ_{AGGREGATE}` state only if this aggregate is locked by a downstream saga's read-then-lock step

### `aggregate/sagas/factories/SagasXxxFactory.java`
- `@Service @Profile("sagas")` — NOT implementing an interface
- Three methods: `create{Aggregate}(...)`, `create{Aggregate}Copy(Saga{Aggregate} existing)`, `create{Aggregate}Dto({Aggregate} agg)`

### `aggregate/sagas/repositories/{Aggregate}CustomRepositorySagas.java`
- `@Service @Profile("sagas")` — concrete class, NOT an interface
- Has `@Autowired {Aggregate}Repository` field
- For aggregates with no custom cross-table lookups, the class body may be empty beyond the autowired field

### `service/{Aggregate}Service.java`
- `@Service`; dependencies constructor-injected (repository, unitOfWorkService); factory `@Autowired`
- Every method has `@Transactional(isolation = Isolation.SERIALIZABLE)`
- Create: generates ID via `aggregateIdGeneratorService`, calls factory, calls `registerChanged`
- Mutate methods: load via `aggregateLoadAndRegisterRead`, create copy via factory, mutate copy, call `registerChanged` on the copy (NOT the original)
- Delete: load via `aggregateLoadAndRegisterRead`, call `aggregate.remove()`, call `registerChanged` on the SAME instance (in-place mutation — intentional exception to copy-on-write)
- P3 rule guards: placed in the service method body (own-table uniqueness check or DTO field validation)
- UpdateCourse specifically: if COURSE_NAME_FINAL and COURSE_TYPE_FINAL are P1 final fields, the update method should throw `{AppClass}Exception({AppClass}ErrorMessage.XXX)`

### `messaging/{Aggregate}CommandHandler.java`
- `@Component` (not `@Service`)
- Extends `CommandHandler`
- `getAggregateTypeName()` returns the PascalCase class name (e.g., `"Course"`)
- `handleDomainCommand(Command command)` uses a switch expression covering every command class
- Default branch logs a warning

### `coordination/functionalities/{Aggregate}Functionalities.java`
- `@Service`
- One method per functionality (write + read)
- Each method: derives functionality name, creates `SagaUnitOfWork`, instantiates `*FunctionalitySagas` inline with `new` (NOT via Spring injection), calls `executeWorkflow`, returns result from saga getter

### `coordination/sagas/{Op}FunctionalitySagas.java`
- Extends `WorkflowFunctionality`
- Constructor calls `buildWorkflow(...)`
- `buildWorkflow()` creates `new SagaWorkflow(this, unitOfWorkService, unitOfWork)`
- Write sagas acting on an existing aggregate: two-step pattern
  - Step 1: wraps read command in `SagaCommand` + calls `setSemanticLock(state)` on the aggregate; compensation releases lock
  - Step 2: sends the mutate command; declares step 1 as dependency
- Create sagas: single step sending the create command, no lock needed
- Read sagas: single step sending the read command, result stored in instance field, exposed via getter

### `commands/{Op}Command.java`
- Extends `Command`
- Constructor calls `super(unitOfWork, serviceName, aggregateId)`
- Uses `ServiceMapping.{AGGREGATE}.getServiceName()` for `serviceName`
- No business logic

### Test files (structural check only — content reviewed in Step 7)
- `{Aggregate}Test.groovy`: in `{tgt-test}sagas/{aggregate}/`; `@DataJpaTest`, `@Transactional`, `@Import(LocalBeanConfiguration)`; inner `LocalBeanConfiguration extends BeanConfigurationSagas`
- Coordination tests: in `{tgt-test}sagas/coordination/{aggregate}/`; same annotations

---

## Step 5: Functionality Coverage

Source the expected operations from plan.md (write + read functionalities for this aggregate).

| Operation | In Service | Saga Class Exists | CommandHandler Case | Functionalities Method | T2 Test Exists | Notes |
|-----------|-----------|------------------|--------------------|-----------------------|---------------|-------|

For each expected operation, check all five columns. Flag any operation failing any check as a finding.

---

## Step 6: Rule Enforcement

For each rule extracted in Step 2.b:

| Rule | Classification | Expected Implementation | Actual Implementation | Status |
|------|---------------|------------------------|----------------------|--------|

Checks by classification:
- **P1 final-field**: field is declared `final` in `{Aggregate}.java`
- **P1 intra-invariant**: condition appears in `verifyInvariants()` throwing `{AppClass}Exception`
- **P2 (consumer)**: `getEventSubscriptions()` includes the subscription; event handler class exists (session 2.N.d)
- **P3**: guard appears in the service method body
- **P4a**: saga fetches the expected upstream aggregate via the correct command
- **P4b**: saga passes the same value to both aggregates during construction

---

## Step 7: Test Coverage

For each test class found in the target:

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|

Expected scenarios per type (from `docs/concepts/testing.md`):

**T1 (`{Aggregate}Test.groovy`):**
- One "create with valid data" case
- One case per P1 intra-invariant (conditions checked in `verifyInvariants()`, not simply final fields)

**T2 — write operations:**
- Happy path ("success")
- One case per P3 guard violation
- One step-interleaving case per saga step that calls `setSemanticLock` (create ops with no lock step need no interleaving case)

**T2 — read operations:**
- Happy path
- Not-found case: must use `thrown(SimulatorException)`, NOT `thrown({AppClass}Exception)` (not-found is infrastructure, not domain)

**T3 (`{Aggregate}InterInvariantTest.groovy`):** only expected if aggregate has P2 subscribed events (session 2.N.d). For each subscribed event:
- "consumer reflects event" (cached field updated)
- "consumer ignores event for unrelated entity" (cached field unchanged)

Flag any test using `thrown({AppClass}Exception)` for a not-found assertion — this is incorrect per `docs/concepts/testing.md`.

---

## Step 8: Retro Cross-Reference

List all action items from the retros found in Step 2.c.

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|

For High-priority items targeting a source file, read the relevant file and verify the fix is present; raise a Major action item if unresolved. Items targeting skill or doc files require no investigation.

---

## Step 9: Build and Test

Build the test-class name list from all test files found in Step 2.e (comma-separated class names).

Run from the project root:
```bash
cd applications/{app-name} && mvn clean -Ptest-sagas test -Dtest="{Aggregate}Test,Create{Aggregate}Test,Update{Aggregate}Test,Delete{Aggregate}Test,Get{Aggregate}ByIdTest" 2>&1 | tail -80
```

Adjust the `-Dtest=` list to match the actual test class names found in Step 2.e.

Capture and record:
- BUILD SUCCESS or BUILD FAILURE
- Per-class pass/fail
- Any compilation errors (quote the relevant lines — these are Critical action items)

---

## Step 10: Write the Review File

Create `{review-dir}` if it does not exist (`mkdir -p {review-dir}`).
Write `{review-file}` using the template below. Do not omit any section — write "nothing to report" if a section has no findings.

```markdown
# Review — {Aggregate}

**App:** {app-name}
**Aggregate:** {Aggregate} (aggregate #{N} in plan.md)
**Date:** {today}
**Verdict:** Green | Yellow | Red

> **Green** = all structural checks pass, all planned operations implemented, tests present and correct, build passes.
> **Yellow** = minor deviations or missing test coverage; build passes.
> **Red** = missing operations, incorrect patterns, broken build, or multiple high-severity issues.

---

## Summary

(One paragraph: overall quality verdict with justification. Reference the most significant findings.)

---

## File Inventory

| File | In Reference | In Target | Status | Notes |
|------|-------------|-----------|--------|-------|

---

## Structural Review

(One subsection per file reviewed. For each: expected pattern with source, actual finding with quoted
lines, status.)

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T2 Test | Notes |
|-----------|-----------|-----------|--------------------|-----------------------|---------|-------|

---

## Rule Enforcement

| Rule | Classification | Expected Impl | Actual Impl | Status |
|------|---------------|--------------|-------------|--------|

---

## Test Coverage

| Test Type | Class | Scenarios Present | Missing Scenarios | Notes |
|-----------|-------|------------------|-------------------|-------|

---

## Retro Cross-Reference

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|

---

## Build & Test Results

**Command:** `mvn clean -Ptest-sagas test -Dtest=...`
**Outcome:** BUILD SUCCESS / BUILD FAILURE

| Test class | Result | Failures |
|------------|--------|---------|

(Full error excerpt if BUILD FAILURE)

---

## Action Items

| Priority | Category | File | Finding | Fix |
|----------|---------|------|---------|-----|
| Critical | ... | ... | ... | ... |
| Major | ... | ... | ... | ... |
| Minor | ... | ... | ... | ... |

**Critical** = breaks correctness or compilability; must fix before next session.
**Major** = incorrect pattern, missing documented test scenario, unresolved High-priority retro item.
**Minor** = naming deviation, unjustified extra file, cosmetic issue.
```

---

## Step 11: Print Summary to Conversation

Output to the conversation:
1. Absolute path to the review file
2. Verdict (Green / Yellow / Red) and one-sentence justification
3. All Critical action items verbatim from the table
4. Count of Major and Minor items
5. Build result: PASS / FAIL, number of test classes run and passed

---

## Hard Rules

1. **Read files directly.** Every comparison is based on actual file content read in Step 2. Never infer the content of an unread file.
2. **Read-only except for the review file.** Do not modify any source or skill files.
3. **Never omit sections.** Write "nothing to report" if a section is empty.
4. **Quote the evidence.** For every Incorrect or Pattern-missing finding, include the relevant snippet from the target file and the expected pattern.
5. **Retro items are mandatory checks.** Every High-priority retro action item targeting a source file must be explicitly resolved or flagged in Step 8.
6. **Build must run.** Do not skip Step 9.
7. **One aggregate per invocation.**
8. **`causal/` files are always Intentional.** quizzes-full is sagas-only. Never raise action items for absent TCC/causal files.
9. **quizzes reference has a different domain.** The reference `CourseService` has `createCourseRemote`, `getAndOrCreateCourseRemote`, etc. — these reflect a design where Course is created indirectly via CourseExecution. For quizzes-full, Course is first-class. Compare service methods by purpose (create / update / delete / read), not by name.
10. **No emojis. Terse and specific.** File paths, method names, rule names, line numbers where relevant.
