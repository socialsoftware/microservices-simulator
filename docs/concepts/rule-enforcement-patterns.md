# Rule-Enforcement Patterns

This document is for **AI agents and developers**. It defines the five patterns used to enforce domain rules, and provides a decision guide for classifying each rule into the correct pattern before writing any code.

Human domain experts define the rules in the domain model template (`{App}-domain-model.md`). The AI agent's job is to read those rules and decide *how* to implement them — not to invent or change the rules themselves.

---

## Quick Reference (AI Agent Entry Point)

### Enforcement Patterns (produce explicit code)

| Rule type | Pattern | Implemented by | Consistency |
|-----------|---------|----------------|-------------|
| Single-aggregate state rule (§3.1) | P1 | `verifyInvariants()` in aggregate | Strong |
| Synchronous service-level check (own-table uniqueness OR saga-assembled DTO validation) | P3 | service guard in `*Service.java` | Strong |
| Cache state from another aggregate (no blocking) | P2 | `getEventSubscriptions()` + event handler chain | Eventual |

### Saga-Structural Guarantees (no P1–P3 enforcement code)

| Rule characteristic | Pattern | Code added? | Where it lives |
|---------------------|---------|-------------|----------------|
| Precondition implicit in saga fetch query (query fails if unmet) | P5a | None | saga data-assembly step |
| Invariant holds because same value is passed to two aggregates in the same saga | P5b | None | saga construction |
| Invariant verified by reading an aggregate back after creation within the same saga | P5c | Assertion in saga | saga post-creation step |

---

## Decision Guide

### Step 1 — Classify §3.1 rules (single-entity)

Every rule in §3.1 of the domain model involves only fields of a single aggregate.

→ **Always P1 — implement as an intra-invariant inside `verifyInvariants()`.**

### Step 2 — Classify §3.2 rules (cross-entity)

For each rule in §3.2, follow the flowchart below in order:

```
Does the rule involve only data that lives inside a SINGLE aggregate
(including its cached snapshot fields)?
  YES → P1 — implement in verifyInvariants()
  NO  → continue ↓

Is the precondition implicit in a saga fetch query?
(the query fails when the precondition is unmet — no explicit service check needed)
  YES → P5a — Construction Prerequisite — no enforcement code
  NO  → continue ↓

Is the invariant guaranteed because the saga passes the same value
to two aggregates?
  YES → P5b — Construction Invariant — no enforcement code
  NO  → continue ↓

Can the rule be verified by reading an aggregate back after creation
within the same saga?
  YES → P5c — Post-Creation Saga Validation — assertion added to saga
  NO  → continue ↓

Is the check a synchronous service-level guard?
  (own-table uniqueness, OR validation of a DTO assembled by a saga step — both enforced in *Service.java)
  YES → P3 — service guard
  NO  → continue ↓

Eventually consistent (~1 s lag) is acceptable?
  YES → P2 — inter-invariant (event subscription + handler chain)
  NO  → re-classify as P3 (add a saga data-assembly step to fetch the needed data as a DTO,
        then validate it inside the service method)
```

### Step 3 — P2 rules never block operations

A P2 inter-invariant is eventually consistent. It may only **cache state** in the consumer aggregate — no operation is blocked based on that cached state.

If you concluded that a rule belongs at P2 but also requires blocking an operation, re-classify:
- Blocking must be synchronous → **P3** (add a saga data-assembly step if the data lives in another aggregate; validate it in the service method).
- True eventual consistency is acceptable → **P2**; no guard is added.

### Common Mistakes to Avoid

**Do not duplicate a rule across patterns.**
Once a rule is placed at P1, do not also add it at P3. P1 fires on every commit regardless of which operation caused the change — this is the canonical single location for single-aggregate invariants.

**P2 never blocks operations.**
A P2 check caches state only. If you find yourself wanting to block based on a P2 cached value, use P3 instead.

**Do not call `DateHandler.now()` inside `verifyInvariants()`.**
Use the `lastModifiedTime` field stamped at mutation time (see P1 temporal variant below). Direct use of `DateHandler.now()` makes the check non-idempotent.

**Never validate cross-aggregate constraints in saga code.**
If you need data from another aggregate to enforce a constraint, either (1) structure the saga command so it fails naturally when the precondition is unmet (P5a — preferred), or (2) pass the assembled DTO to your own service method and validate it there (P3). Putting the validation logic directly inside a saga step is wrong: it couples coordination code to domain rules and bypasses the transactional boundary of the service layer.

---

## P1 — Intra-Invariant

**Use when:** The predicate reads only fields owned by a single aggregate — either scalar fields, collections the aggregate directly manages, or cached snapshot fields (denormalised copies of external state stored inside the aggregate).

**Centralization principle:** P1 is the canonical home for aggregate-state invariants. Because `verifyInvariants()` runs on every commit, a P1 check fires uniformly across all operations, eliminating the need to repeat the same logic in service guards or saga steps. **If a rule fits P1, define it only there — do not add the same check at another pattern.**

> **Restriction:** Mutation methods (`add()`, `remove()`, setters) must **not** throw domain exceptions. All state-consistency rules must be placed in `verifyInvariants()`. Throwing from a mutation method bypasses the centralized check and may leave the aggregate in an inconsistent in-memory state when the UoW has already partially applied mutations.

**Implementation recipe:**

1. Add a private boolean method per predicate:
   ```java
   private boolean invariantFieldOrdering() {
       return this.startDate.isBefore(this.endDate);
   }
   ```

2. Override `verifyInvariants()`, AND all predicates together, throw on failure:
   ```java
   @Override
   public void verifyInvariants() {
       if (!(invariantFieldOrdering() && invariantOtherRule())) {
           throw new {App}Exception({EXCEPTION_CONSTANT}, getAggregateId());
       }
   }
   ```

3. The framework calls `verifyInvariants()` after every write (in `SagaUnitOfWorkService`). No manual call needed.

**Exception pattern:** Each invariant has its own `if` block and throws the most descriptive domain-specific exception available. Use `INVARIANT_BREAK` only if no domain-specific constant fits.

### Variant: temporal immutability via `lastModifiedTime`

Some invariants express a transition rule of the form "field X cannot change once condition Y holds at wall-clock time". These cannot call `DateHandler.now()` directly inside `verifyInvariants()` — the check would be non-idempotent across TCC merges.

**Pattern:** the setter stamps the mutation time as a persistent field (`lastModifiedTime`). `verifyInvariants()` then compares `this.lastModifiedTime` against the threshold — never calling `DateHandler.now()` inside the predicate.

Three rules apply when using this pattern:
1. **Setters stamp time:** each guarded setter calls `setLastModifiedTime(DateHandler.now())` before modifying the field.
2. **Copy constructor bypasses setters:** assign guarded fields directly so copying an existing aggregate does not stamp a new `lastModifiedTime`. Copy `lastModifiedTime` directly from `other` as well.
3. **`verifyInvariants()` reads the stored time:** the invariant method uses `this.lastModifiedTime`, not `DateHandler.now()`.

```java
private boolean invariantFieldsFinalAfterThreshold() {
    {Aggregate} prev = ({Aggregate}) getPrev();
    if (prev != null && prev.getThresholdDate() != null && this.lastModifiedTime != null
            && this.lastModifiedTime.isAfter(prev.getThresholdDate())) {
        return Objects.equals(this.fieldA, prev.getFieldA())
            && Objects.equals(this.fieldB, prev.getFieldB());
    }
    return true;
}
```

**Cached-snapshot rules:** When the aggregate stores a denormalised copy of an external field (e.g. `externalAggregate.someField`), the predicate reads only the cached value — no cross-aggregate query at check time. This is still P1 because all referenced data lives inside the aggregate.

---

## P2 — Inter-Invariant (Event Subscription)

**Use when:** The rule tracks whether a *referenced external aggregate* has been deleted or updated. The aggregate cannot check this at write time because the external change happens asynchronously.

**Upstream / Downstream:** If aggregate A caches state from aggregate B, then B is upstream of A. `getEventSubscriptions()` always lives in the **downstream (consumer) aggregate**. The upstream (publisher) aggregate must not subscribe to its own events.

**Implementation recipe:**

**Step 1 — Create a subscription class:**
```java
public class {Aggregate}Subscribes{EventName} extends EventSubscription {
    private {OwnedEntity} ref;

    public {Aggregate}Subscribes{EventName}({OwnedEntity} ref) {
        super(ref.getExternalAggregateId(), ref.getExternalVersion(),
              {EventName}.class.getSimpleName());
        this.ref = ref;
    }

    @Override
    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event)
            && this.ref.getExternalAggregateId().equals(event.getPublisherAggregateId())
            && this.ref.isActive();
    }
}
```

**Step 2 — Register subscriptions in the aggregate:**
```java
@Override
public Set<EventSubscription> getEventSubscriptions() {
    Set<EventSubscription> subs = new HashSet<>();
    if (getState() == ACTIVE) {
        for ({OwnedEntity} item : this.items) {
            subs.add(new {Aggregate}Subscribes{EventName}(item));
        }
    }
    return subs;
}
```
Only ACTIVE aggregates subscribe. Generate one subscription per referenced object (one per item, per topic, etc.).

**Step 3 — Poll for matched events:**
```java
@Component
public class {Aggregate}EventHandling {
    @Scheduled(fixedDelay = 1000)
    public void handle{EventName}Events() {
        eventApplicationService.handleSubscribedEvent({EventName}.class,
            new {EventName}Handler(repository, {aggregate}EventProcessing));
    }
}
```

**Step 4 — Delegate to the functionality layer:**
```java
@Service
public class {Aggregate}EventProcessing {
    public void process{EventName}(Integer aggregateId, {EventName} event) {
        {aggregate}Functionalities.handleExternalChange(
            aggregateId, event.getPublisherAggregateId());
    }
}
```

---

## P3 — Service Guard

**Use when:** The rule requires a **synchronous check in the service layer** — either:
1. A **global uniqueness constraint** across all instances of an aggregate (own-table read before mutation), or
2. A **DTO field validation** — the saga assembles a DTO from another aggregate in a preceding step and the service method validates a field on that DTO before proceeding.

Both sub-cases execute inside `@Transactional(SERIALIZABLE)` in `*Service.java`, before any aggregate mutation. The data source (own table vs. saga-assembled DTO) does not change the pattern — both are P3.

**When to use P3 vs P2 for uniqueness:**
- Same aggregate's table → P3 (local, transactionally atomic).
- Different aggregate → P2 event cache (eventually consistent); if synchronous enforcement is needed, add a saga data-assembly step to fetch the data as a DTO, then validate it as a P3 check inside the service method.

Placing a uniqueness check at P2 when the authoritative data is local is a mistake: the event cache lags behind reality and allows duplicates in a narrow race window.

**When to choose P5a over P3 for cross-aggregate membership checks:**
If the saga already sends a command to fetch an entity by a compound key (e.g., student by executionId + userId) and that command throws when the entity doesn't exist, no explicit P3 check is needed — the command failing IS the enforcement (P5a). Always prefer P5a when the saga query can be constructed to fail naturally.

**Implementation recipe — own-table uniqueness:**

```java
public void create{Aggregate}({CreateDto} dto) {
    if ({aggregate}Repository.existsByUniqueFields(dto.getFieldA(), dto.getFieldB())) {
        throw new {App}Exception(DUPLICATE_{AGGREGATE});
    }
    // proceed with command
}
```

**Implementation recipe — DTO field validation (saga-assembled data):**

```java
// The saga fetches {OtherAggregate}Dto in a preceding step, then passes it to this method.
public void create{Aggregate}({CreateDto} dto, {OtherAggregate}Dto otherDto) {
    if (!otherDto.isActive()) {
        throw new {App}Exception(INACTIVE_{OTHER_AGGREGATE}, otherDto.getAggregateId());
    }
    // proceed
}
```

---

---

## P5 — By Construction (Saga-Structural Guarantees)

**Use when:** The rule holds automatically because of how the saga is structured. No P1–P3 enforcement code is added.

**Documentation obligation:** Always add a short comment at the saga step explaining why no explicit check exists, so future readers do not add a redundant guard.

### P5a — Construction Prerequisite

The precondition is **implicit in a saga fetch query**: the command fails (throws) if the precondition is not met, aborting the saga before any mutation.

**When it applies:** The fetch only succeeds when the precondition holds — no separate guard is needed. Prefer P5a over P3 whenever a compound-key command can naturally encode the constraint as a failure.

**Canonical example — membership check:** "creator must be enrolled in the execution" is enforced by sending `GetStudentByExecutionIdAndUserIdCommand(executionId, userId)`. The ExecutionService throws `COURSE_EXECUTION_STUDENT_NOT_FOUND` if that student is not enrolled — no explicit P3 check is needed.

```java
// Fetching this DTO enforces the precondition implicitly.
// If the precondition is not met, the command throws and the saga aborts — {RULE_NAME}.
{OtherAggregate}Dto otherDto = commandGateway.send(
    new Get{OtherAggregate}ByConditionCommand(...));
```

### P5b — Construction Invariant

The invariant holds because the saga passes the **same value** to two aggregates in the same workflow. There is no possible inconsistency given a correct saga.

```java
// Both aggregates receive the same sharedId — {RULE_NAME} holds by construction.
commandGateway.send(new Create{AggA}Command(sharedId, ...));
commandGateway.send(new Create{AggB}Command(sharedId, ...));
```

### P5c — Post-Creation Saga Validation

The property can only be verified after a second aggregate is created. The saga reads the aggregate back and asserts the property before continuing.

```java
// Read {Aggregate} back after creation and verify {RULE_NAME}.
{Aggregate}Dto created = commandGateway.send(new Get{Aggregate}ByIdCommand(newId));
if (created.getSomeCount() != expectedCount) {
    throw new {App}Exception({VALIDATION_EXCEPTION});
}
```

---

## Enforcement Lifecycle

Order in which patterns fire within a single operation:

```
Request arrives
      │
      ▼
[saga data-assembly steps]
    (fetch DTOs from upstream aggregates — P5a/P5b guarantees satisfied here)
      │
      ▼
[P3] Service-layer guard
    (in *Service.java — own-table reads / uniqueness, or DTO field validation from saga step;
     runs inside @Transactional(SERIALIZABLE), before any aggregate mutation)
      │
      ▼
[P1] Intra-invariant check
    (verifyInvariants() at UoW commit)
      │
      ▼
    commit
      │
      ▼  (async, ~1 s poll interval)
[P2] Inter-invariant event handler
    (consumer caches publisher state update)
```

P3 and P1 are synchronous and strongly consistent. P2 is asynchronous and eventually consistent.
P5c (post-creation saga validation) fires after the mutation step but before the saga ends — inside the saga, after the create command completes.
