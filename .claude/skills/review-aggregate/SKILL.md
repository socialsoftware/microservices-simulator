---
name: review-aggregate
description: Deep review of one aggregate implementation in {app-name} against its plan.md specification, docs/architecture.md package layout, and the core concept docs. Reads all relevant files, checks structural correctness, functionality completeness, rule enforcement, and test coverage, and runs the build. Writes a structured review to applications/{app-name}/reviews/review-{Aggregate}.md. Invoke with /review-aggregate <AggregateName> (e.g., /review-aggregate Course).
argument-hint: "<AggregateName> (e.g. Course, CourseExecution, Tournament)"
---

# Review Aggregate

Deep, file-by-file review of one aggregate implementation against its `plan.md` specification,
`docs/architecture.md` package-layout conventions, and the core concept docs. Produces a
structured review report and a prioritised action-item list. No reference app is required or
consulted — this skill checks the target app against its own spec and the docs, so it applies
equally to any app built on this harness.

One aggregate per invocation. Reads files directly from disk — does not synthesise from conversation
context.

**See also:** `.claude/skills/adversarial-review-aggregate/SKILL.md` runs second in the same phase and
owns everything this skill deliberately does not check:

- **Semantic correctness.** This skill's checks are presence/shape checks — a `verifyInvariants()`
  with the right shape but an inverted predicate passes Step 6 here.
- **R1-R8 architectural conformance** (`docs/architecture.md` § Architectural Restrictions) against
  code.
- **The correctness of `plan.md` itself.** This skill treats plan.md as authoritative, so a rule
  misclassified in Phase 1 is invisible to it.
- **Manual semantic-lock release in `registerCompensation`.** Owned by that skill's Family C, which
  traces every saga exit path and proves a leaked lock with a fault-injection test. A shape check here
  cannot distinguish a manual release from a legitimate domain-level undo, so it is not duplicated
  (`docs/concepts/sagas.md` § Semantic-lock release on abort is automatic).

Do not expand this skill into those areas — the two are kept separate so the adversarial pass runs in
a fresh context, unanchored by this review's verdict.

---

## Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

## Step 1: Resolve Context

Read `.claude/skills/_shared/conventions.md` § "Resolve app context" and § "Resolve aggregate
context". Derive `{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{aggregate}`, `{N}`,
`{tgt-src}`, `{tgt-test}`, `{review-dir}`.

Additionally derive:

```
{review-file} = {review-dir}review-{Aggregate}.md
```

---

## Step 2: Read Context Files

Read each file and hold it in context. Absent files are findings, not errors — do not skip them.
Read as many files in parallel as possible.

### 2.a — plan.md aggregate section

Read the entire `### {N}. {Aggregate}` section from plan.md. Extract and hold:
- Write and read functionalities list
- Events published / subscribed
- Cross-aggregate prerequisites (P4a/P4b rules)
- Files-to-produce table (sessions 2.N.a through 2.N.d) — this is the **authoritative expected-file
  list** for Step 3
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

### 2.d — Target app files for this aggregate

Enumerate all files under `{tgt-src}microservices/{aggregate}/` recursively.
Also: `find applications/{app-name}/src/main/java -path "*/commands/{aggregate}/*" -name "*.java"`
Also: `find {tgt-test} -path "*/{aggregate}*" -name "*.groovy"` (covers `sagas/{aggregate}/` and `sagas/coordination/{aggregate}/`)

Read each file. Build the target file list.

### 2.e — Concept docs

Read sections relevant to this aggregate type:
- `docs/architecture.md` § Package Structure Convention — canonical directory layout, used to sanity-check
  file placement in Step 3
- `docs/concepts/aggregate.md` — base class, SagaAggregate, verifyInvariants, factories, repositories
- `docs/concepts/service.md` — method patterns, copy-on-write, delete exception, P3 guard placement
- `docs/concepts/commands.md` — command structure, ServiceMapping, CommandHandler, `getAggregateTypeName()`
- `docs/concepts/sagas.md` — SagaWorkflow, SagaStep, lock-acquisition, SagaCommand wrapping
- `docs/concepts/testing.md` — T1–T4 structures, test locations, not-found assertions
- `docs/concepts/events.md` — only if this aggregate has subscribed events (session 2.N.d)

---

## Step 3: File Inventory

The **expected file list** for this aggregate is the Files-to-produce table read in Step 2.a — one
row per file, tagged with the producing session (2.N.a/b/c/d). This table is authoritative for what
this aggregate needs; `docs/architecture.md` § Package Structure Convention (Step 2.e) is used only
to sanity-check *where* a listed file lives (correct layer directory), not to add files the plan.md
table doesn't list.

For each file in the plan.md Files-to-produce table, determine whether it exists in the target file
list (Step 2.d). Match by the relative path as given in plan.md (e.g. `aggregate/Course.java`).

| File (relative to microservices/{aggregate}/) | Session | In Target | Status | Notes |
|------------------------------------------------|---------|-----------|--------|-------|

Status values: `OK` / `Missing` / `Wrong location` / `Extra`

Use `Wrong location` when the file exists under a different directory than either plan.md states or
`docs/architecture.md` § Package Structure Convention implies for its layer — quote both paths.

For target files not listed in plan.md's Files-to-produce table, mark `Extra` with a justification
note (check `docs/architecture.md`'s canonical layout first — a file the convention implies but
plan.md omitted is not automatically unjustified).

---

## Step 4: Structural Review

For each file present in the target, compare its structure against `docs/architecture.md` and the
concept docs (Step 2.e), and against the expectations recorded from plan.md (Step 2.a-b). For every
finding, provide: the file, the expected pattern (with source), the actual finding (quoted
lines), and a status.

Status values: `Correct` / `Minor deviation` / `Incorrect` / `Pattern missing`

### `aggregate/{Aggregate}.java`
- Extends `Aggregate` (not another base class)
- Has `@Entity` annotation
- P1 final-field rules: each such field is declared `final` in Java
- `verifyInvariants()` throws `{AppClass}Exception` for each P1 intra-invariant
- `getEventSubscriptions()` matches `docs/concepts/aggregate.md` § getEventSubscriptions() Implementation (ACTIVE guard, empty set when no subscriptions, one helper per inter-invariant)
- Copy constructor copies all fields including any cached snapshot fields

### `aggregate/sagas/Saga{Aggregate}.java`
- Extends `{Aggregate}` and implements `SagaAggregate`
- `sagaState` field typed as `SagaState` (the interface), NOT the concrete enum
- Default constructor: `sagaState = GenericSagaState.NOT_IN_SAGA`
- Copy constructor: either resets `sagaState = GenericSagaState.NOT_IN_SAGA` or copies
  `other.getSagaState()` — both are valid designs; record which one this target app uses and check
  it is applied consistently across all `Saga{Aggregate}` classes in the app.

### `aggregate/sagas/states/{Aggregate}SagaState.java`
- Implements `SagaAggregate.SagaState`
- Contains `IN_{OPERATION}` states only for write ops that lock an existing instance (not for create-new)
- Contains `READ_{AGGREGATE}` state only if this aggregate is locked by a downstream saga's read-then-lock step

### `aggregate/sagas/factories/SagasXxxFactory.java`
- `@Service @Profile("sagas")` implements `{Aggregate}Factory` (abstract interface defined in `aggregate/` — enables profile-agnostic service injection)
- Three methods: `create{Aggregate}(...)`, `create{Aggregate}Copy(Saga{Aggregate} existing)`, `create{Aggregate}Dto({Aggregate} agg)`

### `aggregate/sagas/repositories/{Aggregate}CustomRepositorySagas.java`
- `@Service @Profile("sagas")` — concrete class, NOT an interface
- Has `@Autowired {Aggregate}Repository` field
- For aggregates with no custom cross-table lookups, the class body may be empty beyond the autowired field

### `service/{Aggregate}Service.java`
- `@Service`; dependencies constructor-injected (repository, unitOfWorkService); factory `@Autowired`
- Every method has `@Transactional(isolation = Isolation.SERIALIZABLE)`
- Method bodies (create / read / mutate / mutate-with-event-publication / mutate-with-optional-sub-collection) match `docs/concepts/service.md` § Method Patterns
- Copy-on-write: every mutation — including delete — creates a factory copy and calls `registerChanged` on the copy, never the original loaded via `aggregateLoadAndRegisterRead`; see `docs/concepts/service.md` § Copy-on-Write Rule. **Flag in-place delete as Incorrect.**
- P3 rule guards: placed in the service method body (own-table uniqueness check or DTO field validation) per `docs/concepts/service.md` § P3 Guard Placement
- Compare service methods against plan.md's write/read functionality list (Step 2.a) by purpose
  (create / update / delete / read), not by name — plan.md is the source of truth for which methods
  should exist and what each one does, including any documented exceptions (e.g. an update method
  that intentionally throws because its target fields are P1 final).

### `messaging/{Aggregate}CommandHandler.java`
- `@Component` (not `@Service`)
- Extends `CommandHandler`
- Matches `docs/concepts/commands.md` § Routing Commands (CommandHandler): `getAggregateTypeName()` returns the PascalCase class name (e.g., `"Course"`); `handleDomainCommand(Command command)` uses a switch expression covering every command class with a default branch that logs a warning

### `coordination/functionalities/{Aggregate}Functionalities.java`
- `@Service`
- One method per functionality (write + read)
- Each method: derives functionality name, creates `SagaUnitOfWork`, instantiates `*FunctionalitySagas` inline with `new` (NOT via Spring injection), calls `executeWorkflow`, returns result from saga getter

### `coordination/sagas/{Op}FunctionalitySagas.java`
- Extends `WorkflowFunctionality`
- Constructor calls `buildWorkflow(...)`
- `buildWorkflow()` creates `new SagaWorkflow(this, unitOfWorkService, unitOfWork)`
- Write sagas acting on an existing aggregate follow the two-step lock-acquisition pattern in
  `docs/concepts/sagas.md` § Lock-Acquisition Step Pattern (Two-Step Write Sagas)
- Create sagas: single step sending the create command, no lock needed
- Read sagas: single step sending the read command, result stored in instance field, exposed via
  getter (see `docs/concepts/sagas.md` § Read Functionality Sagas)
- `registerCompensation` bodies: **out of scope here.** Whether a compensation wrongly releases a
  semantic lock is owned by `/adversarial-review-aggregate` Family C
  (`.claude/skills/adversarial-review-aggregate/SKILL.md` § Step 5). Do not flag it.

### `commands/{Op}Command.java`
- Extends `Command`
- Constructor calls `super(unitOfWork, serviceName, aggregateId)`
- Uses `ServiceMapping.{AGGREGATE}.getServiceName()` for `serviceName`
- No business logic

### Test files (structural check only — content reviewed in Step 7)
- `{Aggregate}IntraInvariantTest.groovy`, `{Aggregate}ServiceTest.groovy` (incl. event-publication cases), `{Aggregate}InterInvariantTest.groovy` (subscribers only): in `{tgt-test}sagas/{aggregate}/`; `@DataJpaTest`, `@Transactional`, `@Import(LocalBeanConfiguration)`; inner `LocalBeanConfiguration extends BeanConfigurationSagas`
- Coordination (T4 functionality) tests: in `{tgt-test}sagas/coordination/{aggregate}/`; same annotations

---

## Step 5: Functionality Coverage

Source the expected operations from plan.md (write + read functionalities for this aggregate).

| Operation | In Service | Saga Class Exists | CommandHandler Case | Functionalities Method | T4 Test Exists | Notes |
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

Expected scenarios per tier — full definitions in `docs/concepts/testing.md`:

- **T1** (`{Aggregate}IntraInvariantTest.groovy`): § T1 — Aggregate Test. P1 Java-`final` fields
  require no test coverage at any tier — do not flag missing tests for final-field rules.
- **T2** (`{Aggregate}ServiceTest.groovy` — one class per aggregate, all service methods + event
  publication): § T2 — Service Test and § Not-Found Paths. **Read the service method first** to
  determine Path A vs Path B before flagging either as missing or wrong. Event-publication cases
  (per published event type, payload assertions + one negative no-publish case) are only expected
  if the aggregate publishes events (plan.md's Events published list).
- **T3** (`{Aggregate}InterInvariantTest.groovy`): § T3 — Subscription (Inter-Invariant) Test.
  Only expected if the aggregate has P2 subscribed events (session 2.N.d).
- **T4 write functionality** (`{Operation}{Aggregate}Test.groovy`): § T4 — Functionality Test
  and § Assertion Ownership.
- **T4 read functionality**: happy path only — not-found cases belong in T2, not here.

---

## Step 8: Retro Cross-Reference

List all action items from the retros found in Step 2.c.

| Retro | Session | Priority | Action Item |
|-------|---------|----------|-------------|

For High-priority items targeting a source file, read the relevant file and verify the fix is present; raise a Major action item if unresolved. Items targeting skill or doc files require no investigation.

---

## Step 9: Build and Test

Build the test-class name list from all test files found in Step 2.d (comma-separated class names),
e.g. `-Dtest="{Aggregate}IntraInvariantTest,{Aggregate}ServiceTest,Create{Aggregate}Test,..."`.

Run it following `.claude/skills/_shared/conventions.md` § "Run the test suite". Do not pipe maven
through `tail` — the exit code and the `Tests run:` lines do not survive it.

Capture and record:
- The observed `MAVEN_EXIT` and the surefire totals (tests / failures / errors / skipped)
- Per-class pass/fail, from the surefire report files
- Any compilation errors (quote the relevant lines — these are Critical action items). A non-zero
  `MAVEN_EXIT` with zero surefire failures usually means compilation failed, so no reports were
  written for the affected classes.

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

| File | Session | In Target | Status | Notes |
|------|---------|-----------|--------|-------|

---

## Structural Review

(One subsection per file reviewed. For each: expected pattern with source, actual finding with quoted
lines, status.)

---

## Functionality Coverage

| Operation | In Service | Saga Class | CommandHandler Case | Functionalities Method | T4 Test | Notes |
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
**Maven exit status:** {MAVEN_EXIT}
**Surefire totals:** tests={count} failures={count} errors={count} skipped={count}
**Outcome:** GREEN / RED

| Test class | Result | Failures |
|------------|--------|---------|

(Full error excerpt if RED. GREEN requires exit status `0` **and** zero failures/errors.)

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
5. Build result: the observed `MAVEN_EXIT` and surefire totals, plus number of test classes run and
   passed

---

## Hard Rules

1. **Read files directly.** Every comparison is based on actual file content read in Step 2. Never infer the content of an unread file.
2. **Read-only except for the review file.** Do not modify any source or skill files.
3. **Never omit sections.** Write "nothing to report" if a section is empty.
4. **Quote the evidence.** For every Incorrect or Pattern-missing finding, include the relevant snippet from the target file and the expected pattern.
5. **Retro items are mandatory checks.** Every High-priority retro action item targeting a source file must be explicitly resolved or flagged in Step 8.
6. **Build must run, and its result must be observed, not scraped.** Do not skip Step 9. Report the
   outcome from maven's exit status and the surefire report files
   (`.claude/skills/_shared/conventions.md` § "Run the test suite"), never from piped maven stdout.
7. **One aggregate per invocation.**
8. **No emojis. Terse and specific.** File paths, method names, rule names, line numbers where relevant.
