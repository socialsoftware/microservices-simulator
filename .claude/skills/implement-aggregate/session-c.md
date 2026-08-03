# Session 2.N.c — Write Functionalities

This sub-file is loaded by `implement-aggregate` when the target session type is `c`. All context variables (`{app-name}`, `{pkg}`, `{AppClass}`, `{Aggregate}`, `{N}`, `{src}`, `{test}`, `{bean-config}`) are already available from the router.

> **Slice scope.** You may be assigned a subset of this session's items. If your brief names specific
> functionalities or events, implement only those, and append to the shared files rather than
> rewriting them. If no subset is named, you own the whole session.

---

## Reads

Load these files before writing any code:

1. **Domain files produced in session 2.{N}.a** — read every file listed in the plan.md `2.{N}.a` row for this aggregate. You need the aggregate class structure, field names, constructor signature, and `SagaState` enum values.

2. **Read-layer files produced in session 2.{N}.b** — `{Aggregate}Service.java`, `{Aggregate}CommandHandler.java`, `{Aggregate}Functionalities.java`, `{Aggregate}ServiceTest.groovy` and `Get{Aggregate}ByIdCommand.java`. This session **appends** to all of them; understanding their existing structure is required. `Get{Aggregate}ByIdCommand` and its service method already exist — write sagas consume them in their get-then-lock step and must not re-create them.

3. **`docs/concepts/service.md`** — specifically:
   - § Method Patterns (Read / Create / Mutate / Mutate with event publication / Mutate with optional sub-collection parameter)
   - § Copy-on-Write Rule, § DTO Immutability (R7), § Exception-Throw Convention
   - § P3 Guard Placement — where own-table uniqueness checks and DTO field validation live
   - § Partial-Data Owned Entities (only if the aggregate has owned sub-entities)
   - § Custom Repository — Latest-Active-Version Query (only if returning lists)

4. **`docs/concepts/commands.md`** — specifically:
   - § What a Command Is, § Naming Conventions, § File Location
   - § ServiceMapping Enum (mandatory entry per aggregate)
   - § Sending Commands (Functionality Layer), § Routing Commands (CommandHandler)
   - § Known DTO Gaps and Compensating Command Steps (only if the saga touches a known-gap DTO; see subsection list)

5. **`docs/concepts/sagas.md`** — specifically:
   - § Step Ordering (authoritative; this session no longer restates it)
   - § Lock-Acquisition Step Pattern (Two-Step Write Sagas), § Semantic Locks in Practice
   - § R4 Decision Table — `SagaCommand` vs `setForbiddenStates`
   - § Create Functionality Sagas — only if a write functionality for this aggregate creates it; the create step declares neither a lock nor `forbiddenStates` and instead registers a compensation
   - § Write Workflow Structure

6. **`docs/concepts/testing.md`** — § Assertion Ownership, § T2 — Service Test, § T4 — Functionality Test, § Fake / Wrong / Weak Detection Checklist. This session writes tests in two tiers (T2 write cases + event publication, T4 write functionality) — read the full rule set before writing any test file, apply the Fake/Wrong/Weak checklist before committing each one.

7. ***(Conditional)*** If the plan.md aggregate section lists cross-aggregate prerequisites (P4a or P3 DTO-check rules): read the service file and relevant command files of each upstream aggregate involved. You need their command class names, service method signatures, and what they throw on failure.

---

## Produce

Produce every file listed in the plan.md `2.{N}.c` row. plan.md is a blueprint, not a manifest: the `###` subheadings below are the authority on what this session must emit, and a file they require but plan.md omits is still produced - amend the row per `_shared/session-completion.md` § "Amend plan.md for omitted files". The descriptions below explain what each file must contain.

> **One required edit lives outside this section.** § "Update `{AppClass}SpockTest.groovy`" below
> mandates replacing the `create{Aggregate}()` helper body with the real create functionality. It is
> not a `###` subheading here, so a session that treats § Produce alone as its manifest ships a stale
> placeholder helper and leaves session `b`'s read tests running against fixture-built aggregates
> rather than the create path.

> **Prerequisite — ServiceMapping**: The `{src}ServiceMapping.java` entry for `{AGGREGATE}` was added in session 2.{N}.b, which is where this aggregate's first commands were written. Verify it is present before writing any commands — every command constructor references `ServiceMapping.{AGGREGATE}.getServiceName()`.

> **Prerequisite — Upstream count-manipulation commands**: If any saga for this aggregate sends an `Increment{Xxx}CountCommand` or `Decrement{Xxx}CountCommand` to an upstream aggregate's `CommandHandler`, verify that handler already routes the command. If the case is missing, add it before running tests — an unrouted command silently does nothing and will cause invariant violations or state corruption that are difficult to diagnose after the fact.

### `{Aggregate}Service.java` (write methods)

Path: `{src}microservices/{aggregate}/service/{Aggregate}Service.java`

- Spring `@Service`. The class already exists from session 2.{N}.b — **append** the write methods, do not rewrite the file.
- One method per write functionality listed in plan.md
- Method signature: receives the command's fields + `UnitOfWork unitOfWork`
- **P3 own-table uniqueness guards** (if listed in plan.md P3 rules): query the repository for duplicates before creating; throw `{AppClass}Exception` with the appropriate error message constant if found
- **P3 DTO field checks** (if listed in plan.md cross-aggregate prerequisites): receive the saga-assembled DTO as a parameter; validate the field; throw `{AppClass}Exception` on violation
- After validation, follow `docs/concepts/service.md` § Method Patterns exactly. The call shape is:
  load with `unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork)`, create a
  factory copy with `{aggregate}Factory.create{Aggregate}Copy(old)`, apply the setters **to the
  copy**, then `unitOfWorkService.registerChanged(copy, unitOfWork)`. Do not mutate the loaded
  instance (`docs/concepts/service.md` § Copy-on-Write Rule), do not load through the custom
  repository for a by-ID mutation, and do not call `verifyInvariants()` yourself — `registerChanged`
  invokes it.
- **Soft-delete** (`remove()`): the same copy-on-write shape, with `copy.remove()` before
  `registerChanged`. Never call `remove()` on the managed entity returned by `aggregateLoadAndRegisterRead`; doing so lets JPA auto-flush the deleted state before the saga abort query runs, making the aggregate invisible to the abort path.
- **Event publishing**: for each event this aggregate publishes (see plan.md Events published), call `unitOfWorkService.registerEvent(new {Event}(...), unitOfWork)` at the end of the relevant service method

> **Deferred P3 guards:** If a P3 DTO-check rule listed in plan.md cross-aggregate prerequisites requires data from an aggregate ordered _after_ this one in plan.md (because that later aggregate subscribes to this one's events), the guard cannot be implemented yet. Do the following:
> 1. **Skip** the data-assembly saga step and the service guard — do not add stubs.
> 2. Add a `// TODO: {RULE_NAME} — deferred; requires {LaterAggregate}Dto, available after session 2.{M}.c` comment in the `{Op}FunctionalitySagas` class at the exact location where the data-assembly step will be inserted.
> 3. Flag the deferral explicitly in the session retro.
> 4. When session 2.{M}.c completes, revisit this saga and add the data-assembly step and service guard.
>
> This situation arises when there is a bidirectional dependency between two aggregates: the topological sort correctly prioritizes the event-subscription direction, and the P3 read-time reverse dependency is the deferred consequence. The plan.md for such rules should carry a ⚠️ DEFERRED marker (added by classify-and-plan) — if you see it, this guidance applies.

### `{Aggregate}CommandHandler.java`

Path: `{src}microservices/{aggregate}/messaging/{Aggregate}CommandHandler.java`

- The class already exists from session 2.{N}.b with its read cases — **append** one `switch` case per write command in `handleDomainCommand`, do not rewrite the file
- The two-override shape (`getAggregateTypeName()` plus a single `handleDomainCommand(Command command)` with a `default` branch that logs a warning) is unchanged; see `docs/concepts/commands.md` § Routing Commands (CommandHandler)
- Each case calls the matching service method, passing `cmd.getUnitOfWork()` — the handler does
  **not** create or commit a UnitOfWork; the workflow owns its lifecycle. Mutating cases
  `yield null`; read cases return the DTO.
- The Spring **bean name** must be `ServiceMapping.{AGGREGATE}.getServiceName() + "CommandHandler"`
  (lowercase camelCase, e.g. `shipmentItemCommandHandler`) — that is the actual routing key

### One `{Op}{Aggregate}Command.java` per write functionality

Path: `{src}commands/{aggregate}/{Op}{Aggregate}Command.java` — rooted at the **app source root**, not
at `microservices/{aggregate}/`. See `docs/concepts/commands.md` § "File Location".

- Implements `Command`
- Fields: all parameters needed by the service method
- Constructor, getters
- Name convention: operation in PascalCase + aggregate name + `Command` (e.g., `CreateShipmentCommand`)

### One `{Op}FunctionalitySagas.java` per write functionality

Path: `{src}microservices/{aggregate}/coordination/sagas/{Op}FunctionalitySagas.java`

- Extends `WorkflowFunctionality` (`pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality`)
- The constructor calls `buildWorkflow(...)`; `buildWorkflow` assigns
  `this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork)`, declares each `SagaStep`
  with its dependency list, and registers them with `this.workflow.addStep(...)` — see
  `docs/concepts/sagas.md` § Write Workflow Structure
- **Step ordering, lock-step pattern, R4 foreign-vs-primary distinction, and R8 upstream-only rule** — follow `docs/concepts/sagas.md` § Step Ordering (and the linked § Lock-Acquisition Step Pattern, § R4 Decision Table). That section is authoritative; do not re-derive the order from the implementation.
- **Compensations (`registerCompensation`) are for genuine domain-level undos only** — reversing a real side effect a step produced (e.g. deleting a child aggregate a step created). Never register a compensation to release a semantic lock: lock release on abort is automatic, and a manual release re-locks the aggregate (see `docs/concepts/sagas.md` § Semantic-lock release on abort is automatic).
- The **conditional validate-dates step** is per-aggregate guidance, not a generic pattern: if `plan.md` for this aggregate lists time-based invariants on both this aggregate and a downstream aggregate created in the same saga, insert the validate-dates step first.

### `{Aggregate}Functionalities.java`

Path: `{src}microservices/{aggregate}/coordination/functionalities/{Aggregate}Functionalities.java`

- Spring `@Service`. The class already exists from session 2.{N}.b with its read coordinator methods — **append** the write methods, do not rewrite the file.
- One public method per write functionality (matching the saga class name)
- Each method:
  1. Creates a `SagaUnitOfWork` with `unitOfWorkService.createUnitOfWork("{operationName}")`, passing
     the operation name as a **string literal** matching the method name — the same idiom session `b`
     uses for the read coordinators in this file. Do not derive it reflectively from the stack trace:
     both forms evaluate to the same string, but a session `c` that appends to a session-`b` file
     would leave one class carrying two idioms for one thing.
  2. Instantiates the corresponding `{Op}FunctionalitySagas` directly (not as a Spring bean)
  3. Calls `executeWorkflow(uow)` on it
  4. Returns the result DTO (or `void` for mutations)
- Tests `@Autowired` this class and call its methods directly

### `{Aggregate}ServiceTest.groovy` (T2 — write-method cases)

Path: `{test}sagas/{aggregate}/{Aggregate}ServiceTest.groovy`

One class per aggregate, covering all its service methods. The class already exists from session
2.{N}.b with its read-method cases — **append** the write-method cases, do not rewrite the file.
Follow the template in `docs/concepts/testing.md` § T2 — Service Test. Invoke the
`*Service` bean directly with a `UnitOfWork` — no saga workflow, no `{Aggregate}Functionalities`.

- Extends `{AppClass}SpockTest`
- **Per write service method, a happy path**, per `testing.md` § T2 — Service Test (read-back rules)
  and § Fake / Wrong / Weak Detection Checklist (fresh-UnitOfWork and kill-mutation smells).
- **Uniqueness / composite-key guard cases**: one per P3 own-table or DTO-check guard in the
  service method.
- **P3 numeric-guard boundaries**: one on-point/off-point pair per ordered-domain P3 guard, per
  `testing.md` § Choosing Input Values — EP & BVA.
- **P1 intra-invariants are not tested here** — they belong in `{Aggregate}IntraInvariantTest.groovy`
  (session a, T1).
- `// Spec:` comment on every test naming the plan.md section and rule (see Spec-First note below).

**Event-publication assertions (only if plan.md lists events published):** appended to the same
`{Aggregate}ServiceTest.groovy` class as separate `def` methods (not folded into existing `then:`
blocks — event-store facts and persisted-state facts stay separate assertions). Follow the
template in `docs/concepts/testing.md` § T2 — Service Test. Autowire `EventService`. Trigger the
publishing operation **via a direct service call** with a `UnitOfWork` (not via
`{Aggregate}Functionalities`), then assert against the event store via the `EventService` bean.

- **Per published event type** (from plan.md's Events published list): one case asserting the
  event exists with the correct type, `publisherAggregateId`, and **every payload field** —
  asserting only type/count is **Weak**.
- **One negative case**: capture the event-store count before, run a service operation that must
  *not* publish, assert the count is unchanged.
- Consumers are out of scope here — they are covered by T3 subscription tests in session `d`.

### One `{Op}Test.groovy` per write functionality (T4)

Path: `{test}sagas/coordination/{aggregate}/{Op}Test.groovy`

> **Anti-pattern:** Do not read `{Aggregate}Service.java` or `{Op}FunctionalitySagas.java` to decide what to assert. Tests derived from the implementation you just wrote are tautological — they verify what the code does, not what the domain says it should do. The remedy is the spec table below.

**Cite plan.md as the spec — do not author a parallel artifact.** The `plan.md` aggregate section for the target aggregate already contains the happy-path postconditions, the events-published list, and the P1/P3 rule list. That section *is* the spec; the test asserts it. See `docs/concepts/testing.md` § Spec-First Ordering.

At the top of every happy-path and violation test, write a single-line `// Spec:` comment that names the plan.md section and the rule (or "happy path") the test asserts. Example:

```groovy
def "updateShipmentNotes: SHIPMENT_NOTES_REQUIRED violation"() {
    // Spec: plan.md §3.5 Shipment / functionalities — UpdateShipmentNotes; rule SHIPMENT_NOTES_REQUIRED
    given:
    ...
}
```

If the implementation disagrees with the cited section, flag it as an impl deviation — do not adjust the cited rule to match.

**Strict assertion ownership (testing.md § Assertion Ownership):** T4 functionality tests do **not**
assert field-level persistence, uniqueness, or not-found — those belong in `{Aggregate}ServiceTest`
(T2, above). They also do not re-assert event-store contents — `{Aggregate}ServiceTest` (T2) owns
that.

- Extends `{AppClass}SpockTest`
- **Happy-path test**: set up prerequisites using `{AppClass}SpockTest` helpers, execute the operation via `{Aggregate}Functionalities`, and assert **orchestration outcomes only**: the operation completes, the returned DTO is coherent, and `sagaStateOf(<aggregateId>) == GenericSagaState.NOT_IN_SAGA`
- **Saga-path guard tests**: P3 guard violations that involve cross-aggregate saga coordination, driven through `{Aggregate}Functionalities` (single-aggregate guard violations are already covered in T2 via direct service calls — do not duplicate them here)
- **P4a prerequisite tests**: test what happens when the upstream fetch fails (e.g., requester not registered in the warehouse)
- **Assertion for all violation tests:** `thrown({AppClass}Exception)` plus `ex.message == {RULE_NAME}`. Never use `thrown(Exception)` — the bare `Exception` is only acceptable in Fault / Behavior Test (Appendix) fault-injection tests. Never accept a bare `thrown({AppClass}Exception)` without the message assertion — it passes on any thrown exception of that type, including unrelated bugs. The `{RULE_NAME}` constant must match the name in `plan.md`'s rule list, not be inferred from the implementation.
- **P1 intra-invariants are not tested here** — they belong in `{Aggregate}IntraInvariantTest.groovy` (session a). Do not add P1 violation tests or BVA boundary straddles to T4 functionality tests.
- **State-transition / semantic-lock acquisition (required):** Follow `docs/concepts/testing.md` § T4 — Functionality Test. Each `setSemanticLock` step is an *acquire* transition into `IN_{OP}`. **One case per saga step that calls `setSemanticLock` — no exceptions:**
  - **`setSemanticLock` step:** run the workflow through the lock step via `executeUntilStep("<lockStep>", uow)`, assert `sagaStateOf(<id>) == <Aggregate>SagaState.IN_<OP>` in `expect:` (the post-*acquire* state), call `resumeWorkflow(uow)` in `when:`, assert `noExceptionThrown()` in `then:` (the traversal completes back to `NOT_IN_SAGA`).
  - Cross-aggregate `setForbiddenStates` conflict validation is **deferred — see Appendix — Cross-Functionality Test** in `docs/concepts/testing.md`.
  - **Coverage is audited mechanically.** List every `setSemanticLock` step (one row per call site) in the session retro's **Semantic-Lock Coverage Audit** table — see `.claude/skills/_shared/session-completion.md` § "Retro template". Unresolved `Present? = No` rows block the session commit.

### One `{Op}CompensationTest.groovy` per lock-holding write functionality (T4)

Paths — **both files are produced**, and the test cannot fault without the second:

```
{test}sagas/coordination/{aggregate}/{Op}CompensationTest.groovy
src/test/resources/groovy/{Op}CompensationTest/{Op}FunctionalitySagas.csv
```

The impairment CSV is selected by the **test class's simple name** (directory) and the **saga class's
simple name** (file). A compensation test written without its CSV injects no fault, so the expected
exception never arrives and the test fails for a reason unrelated to compensation.

After writing it, run the sanity check `docs/concepts/testing.md` § Compensation Test mandates:
temporarily flip the faulted step's flag to `0`, re-run, and confirm from the log that the
lock-acquiring step genuinely executes before the fault fires. A compensation test that has never
been run in both configurations is not known to be testing compensation.

Compensation tests are **core T4 scope**, not deferred. Follow `docs/concepts/testing.md`
§ Compensation Test in full — it owns the shape, the `ImpairmentService` mechanism and the CSV
format — and note in particular its § "CRITICAL gotcha — one saga class, one compensation test file":
the case cannot live inside `{Op}Test.groovy`, because the two need opposite fault state in the same
CSV block.

**Applicability test:** required for every write functionality whose saga holds a semantic lock
**across a later step** — a `setSemanticLock` step with a dependent step registered after it. Skip it
for a functionality whose only step has no dependents (nothing to compensate), and for read
functionalities, which acquire no lock.

A create functionality typically has no compensation test: a single-step create acquires no lock. A
mutate functionality that locks in a `get{Aggregate}Step` and mutates in a dependent step typically
does have one.

Whether each write functionality of this session needs the file is decided by the applicability test
above, per functionality — not once for the session.

### Event classes (if this aggregate publishes events)

For each event listed in plan.md Events published that does not yet exist:

Path: `{src}events/{Event}.java`

- Extends `pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event` from simulator core — it is a JPA `@Entity` and must be annotated `@Entity`. Do **NOT** implement `DomainEvent` directly.
- Fields: all payload fields needed by consumers (check aggregate-grouping §4 event table in domain spec for payload)
- Constructor, getters

### Error message constants

Open `{src}microservices/exception/{AppClass}ErrorMessage.java` and add constants for:
- P3 guard violations introduced in this session
- Any new invariant messages not already added in session `a`

Append to the existing file; do not remove existing constants.

### `{Aggregate}Controller.java`

Path: `{src}microservices/{aggregate}/coordination/webapi/{Aggregate}Controller.java`

- Single `@RestController` annotation; empty body
- Package: `pt.ulisboa.tecnico.socialsoftware.{pkg}.microservices.{aggregate}.coordination.webapi`
- No methods needed — REST endpoints are not exercised by the test harness; this stub marks the architectural slot
- Import: `org.springframework.web.bind.annotation.RestController`

---

## BeanConfigurationSagas — No New Bean, But Check the Existing Ones

The three beans this aggregate needs — `{Aggregate}Service`, `{Aggregate}CommandHandler` and `{Aggregate}Functionalities` — were registered in `{bean-config}` during session 2.{N}.b. This session appends methods to those existing classes, so no *new* bean is required. `{Op}FunctionalitySagas` classes are **not** Spring beans — they are instantiated inline inside `{Aggregate}Functionalities`. **Do not add any `@Bean` method in this session.**

**Existing `@Bean` methods do change.** A service takes every dependency through its constructor and
declares no `@Autowired` field (`docs/concepts/service.md` § Injected Dependencies), so a write method
needing a collaborator the service does not yet hold requires **two** edits, not one: widen
`{Aggregate}Service`'s constructor and field list, **and** widen the matching
`{aggregate}Service(...)` `@Bean` method's parameters and `new {Aggregate}Service(...)` call to pass
it. Update that `@Bean` method in place; never add a second bean for the same class.

The usual case is `AggregateIdGeneratorService`: session `b` omits it when no read method mints an
aggregate id, and this session's create method is the first to need it.

---

## Update `{AppClass}SpockTest.groovy`

Session 2.{N}.b already added a `create{Aggregate}(...)` helper, built directly on the aggregate
because the create functionality did not exist yet. **Replace its body** with the real thing:

```groovy
Integer create{Aggregate}(/* same parameter list and defaults as 2.{N}.b */) {
    def {aggregate}Dto = new {Aggregate}Dto()
    // set the fields from the parameters
    return {aggregate}Functionalities.create{Aggregate}({aggregate}Dto).aggregateId
}
```

**Keep the signature and the defaults exactly as session 2.{N}.b wrote them.** The read tests from
that session call this helper, and a signature change rewrites them for nothing. If the create
functionality genuinely cannot satisfy the existing signature, that is a real mismatch — take it to
`SKILL.md` § "Step 3b: The Self-Healing Gate" rather than silently re-shaping the helper.

Once replaced, the aggregate is created through the real saga, so 2.{N}.b's read tests exercise the
production create path from here on. Re-run them and confirm they still pass.

Tests added this session use the same helper in their `setup:` block to satisfy prerequisites.

---

## Tick the Checkbox

In plan.md, replace:
```
- [ ] 2.{N}.c — Write functionalities
```
with:
```
- [x] 2.{N}.c — Write functionalities
```
