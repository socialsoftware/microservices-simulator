# Retro — 2.8.a — Order

**App:** trainticket
**Session:** 2.8.a (Domain Layer)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/Order.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/SeatClass.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderStatus.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/sagas/SagaOrder.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/sagas/states/OrderSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/sagas/factories/SagasOrderFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/sagas/repositories/OrderCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/OrderServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java` (appended: six Order P1 constants)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/order/OrderIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (appended: Order domain constants and their imports)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy` (appended: `sagasOrderFactory`, `orderCustomRepositorySagas`)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (2.8.a checkbox ticked; 2.8.a file row amended)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants (Sagas), § Factories, § Repositories, § getEventSubscriptions() | Yes | `getPrev()` returning the base `Aggregate` type is documented under § Key Fields; the downcast needed by a `prev`-predicating invariant is left to the implementer but is unambiguous. |
| `docs/concepts/testing.md` | § T1 — Aggregate Test | Partial | The template covers a P1 rule that reads only local fields. Order has two rules (`ORDER_STATUS_TRANSITION`, `ORDER_REFUND_AMOUNT`) that predicate on the previous version, so the test must build a version chain with `setPrev`; no template shows that. |
| `docs/concepts/sagas.md` | § SagaState, § Lock-Acquisition Step Pattern, § Step Ordering, § Create Functionality Sagas | Yes | Settled the locked-state question that session-a.md had contradicted; § "Create Functionality Sagas" confirmed no state for the create operation. |
| `applications/trainticket/trainticket-domain-model.md` | §1, §2, §3.1 | Yes | — |
| `applications/trainticket/trainticket-aggregate-grouping.md` | §2 | Yes | Confirmed the five cached-field groups and that `departureTime` is derived, not copied. |

---

## Skill Instructions Feedback

### What worked well

- § "Verify Mandatory Files in plan.md" - both interface files were already present in the 2.8.a row, so the check was a no-op, which is the intended outcome.
- The `(shared)` enum rule made `DocumentType` unambiguous: the row marks it import-only, the file already existed from 2.3.a, and it was left untouched.
- § "Update `{AppClass}SpockTest.groovy`" is explicit that session `a` adds constants but no `create{Aggregate}` helper; that boundary held cleanly even though Order is the aggregate with the most fields in the application.

### What was unclear or missing

- § "`{Aggregate}SagaState.java`" contradicted `docs/concepts/sagas.md` on which states a two-step write saga needs, and named only `IN_UPDATE_{AGGREGATE}` / `IN_DELETE_{AGGREGATE}` - no rule for an aggregate whose mutating operations are domain-specific verbs. Fixed as a Type 1 (harness-log row 23, commit `265d7ff91`).
- § "`{Aggregate}.java`" says the constructor "accepts all required fields" but says nothing about a field the domain model gives a fixed **default** rather than a caller-supplied value. `Order.status` is always `NOTPAID` at creation, so the creating constructor ignores `orderDto.getStatus()`. Proceeded with a one-line comment recording why the DTO field is not read.
- The skill has no guidance on P1 rules that predicate on `getPrev()`. Both the invariant code (downcast, null-prev branch) and the T1 test (constructing a chained version pair) had to be inferred.

### Suggested wording / structure changes

- `docs/concepts/testing.md` § T1: add a third template shape - "version-chain case" - showing `new Saga{Aggregate}(previous)` plus `setPrev(previous)` for rules whose predicate mentions `prev`.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/testing.md` | § T1 has no template for a P1 rule that predicates on the previous aggregate version; the whole matrix assumes local fields only | Medium | Add a version-chain case shape: build v0, copy-construct v1, `v1.setPrev(v0)`, mutate, `verifyInvariants()` |
| `docs/concepts/aggregate.md` | § Base Class names `prev` as "used for temporal invariants" but shows no invariant that reads it - no guidance on the downcast, the `prev == null` first-version branch, or the risk of a rule re-firing on every later commit | Medium | Add a § "Invariants over the version chain" with the guard shape `status == X && (prev == null \|\| prev.status != X)` in neutral vocabulary |
| `.claude/skills/implement-aggregate/session-a.md` | § "`{Aggregate}.java`" is silent on constructor handling of a field the spec gives a fixed default rather than a caller-supplied value | Low | State that a defaulted field is set from the domain model's default and the DTO's copy of it is ignored on the create path |

---

## Patterns to Capture

- **Pattern:** Transition table as a static `EnumMap<Status, EnumSet<Status>>`
  **Observed in:** `microservices/order/aggregate/Order.java`
  **Description:** A state-machine P1 rule stated as an edge list is enforced by a private static map from source state to permitted target states, built once, with `status == prev.status` short-circuited before the lookup. Keeps `verifyInvariants()` free of a nested switch and makes the edge list readable against the spec table.

- **Pattern:** Confining a version-chain invariant to the transitioning commit
  **Observed in:** `microservices/order/aggregate/Order.java` (`verifyRefundAmount`)
  **Description:** A rule that derives an expected value from `prev`'s state must exclude the case where `prev` is already in the target state, or every later commit on the aggregate re-evaluates it against the wrong branch. The guard is `status == TARGET && prev != null && prev.status != TARGET`.

- **Pattern:** Ordering `verifyInvariants()` so an earlier rule discharges a later rule's precondition
  **Observed in:** `microservices/order/aggregate/Order.java`
  **Description:** The status-transition check runs first, which makes "first version is in a terminal state" unreachable for the refund check below it, so the refund branch needs no null-prev handling. The dependency is recorded in a comment because it is invisible from the refund method alone.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 23

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 23 | 1 | fixed | `265d7ff91` |

---

## One-Line Summary

Order's domain layer is the application's first aggregate whose invariants read the previous version, and neither the T1 template nor the aggregate doc covers that shape - the code and the 30 T1 cases were inferred from the rule text alone.
