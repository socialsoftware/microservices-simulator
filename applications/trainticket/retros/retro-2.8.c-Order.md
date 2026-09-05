# Retro — 2.8.c — Order

**App:** trainticket
**Session:** 2.8.c (Write Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/PreserveTicketCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/PayOrderCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/CollectTicketCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/UseTicketCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/CancelOrderCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/DeleteOrderCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/PreserveTicketFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/PayOrderFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/CollectTicketFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/UseTicketFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/CancelOrderFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/DeleteOrderFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/webapi/OrderController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/service/OrderService.java` (write methods appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/messaging/OrderCommandHandler.java` (write cases appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/functionalities/OrderFunctionalities.java` (write coordinators appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/Order.java` (`cancel(cancelledTime)` mutator added)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java` (four §3.2 constants added)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (`createOrder` rerouted, date and fare constants re-pinned)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/order/OrderServiceTest.groovy` (write-method cases appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/PreserveTicketTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/PayOrderTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/CollectTicketTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/UseTicketTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/CancelOrderTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/DeleteOrderTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/PayOrderCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/CollectTicketCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/UseTicketCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/CancelOrderCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/DeleteOrderCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/PayOrderCompensationTest/PayOrderFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/CollectTicketCompensationTest/CollectTicketFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UseTicketCompensationTest/UseTicketFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/CancelOrderCompensationTest/CancelOrderFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeleteOrderCompensationTest/DeleteOrderFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/GetOrderByIdTest.groovy` — the fare assertion moved from the fixture-supplied `ORDER_PRICE` to the derived `ORDER_BOOKED_PRICE` once `createOrder` began booking through PreserveTicket.

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Method Patterns (Create / Mutate), § Copy-on-Write Rule, § DTO Immutability (R7), § Exception-Throw Convention, § P3 Guard Placement, § Custom Repository | Yes | The soft-delete copy-on-write rationale carried directly to `deleteOrder`. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Sending / Routing Commands | Yes | The `null` root-aggregate id rule already covered `PreserveTicketCommand` as a create. |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas, § Semantic-lock release on abort is automatic, § Write Workflow Structure | Yes | § Create Functionality Sagas Shape 1 settled that PreserveTicket registers no compensation: the create is the last step. |
| `docs/concepts/rule-enforcement-patterns.md` | § P3, § P4a, § P4b, § Common Mistakes to Avoid | Partial | "Never validate cross-aggregate constraints in saga code" settled where the guards go, but P4b is defined as "the same value passed to **two** aggregates" and the two P4b rules here pass one derived value into a **single** aggregate. See Documentation Gaps. |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2, § T4, § Compensation Test, § Fake / Wrong / Weak Checklist, § Choosing Input Values, § Soft-delete happy-path assertion | Partial | The P4a assertion carve-out assumed a primary-key fetch; see Harness Changes row 24. |
| `.claude/skills/implement-aggregate/session-c.md` | whole file | Partial | Same carve-out gap; also see Skill Instructions Feedback on the `create{Aggregate}` helper contract. |

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`" was right to insist the `createOrder` signature stay
  frozen: the 2.8.b read tests were rerouted onto the real booking saga without a single call-site
  edit, and the whole read suite passed unchanged on the first run.
- § "CRITICAL gotcha — one saga class, one compensation test file" prevented the obvious mistake of
  folding the five compensation cases into the five `{Op}Test` classes; the block-index explanation
  made the separation self-evident rather than arbitrary.
- The instruction to sanity-check compensation tests against the real `START EXECUTION STEP` log
  line (via the subprocess capture recipe) was worth doing: it confirmed `getOrderStep` genuinely
  executes before the injected fault in all five sagas, so none of the `NOT_IN_SAGA` assertions is
  trivially satisfied.

### What was unclear or missing

- § "Update `{AppClass}SpockTest.groovy`" freezes the helper's **signature** but says nothing about
  the **constants** its call sites assert against. Rerouting `createOrder` onto PreserveTicket
  changed two things no signature rule covers: the fare stopped being fixture-supplied and became
  saga-derived, and `boughtDate` stopped being a pinned literal and became `DateHandler.now()`, which
  silently invalidated a hard-coded future travel date once real time overtook it. Both are inherent
  to the reroute, not to this domain — any aggregate whose create functionality derives a field the
  fixture used to supply will hit them.
- The session sub-file does not say who derives a P4b value — the workflow class or the service
  method. Here the derivation depends on a lookup that a P3 guard in the service validates
  (the journey endpoints), so deriving it in the workflow would have raised a `NullPointerException`
  ahead of `ENDPOINTS_ON_TRIP_ROUTE`. Resolved by deriving in the service, after the guards, which is
  also what `rule-enforcement-patterns.md` § Common Mistakes implies; but the ordering constraint is
  nowhere stated.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-c.md` § "Update `{AppClass}SpockTest.groovy`": add a
  paragraph stating that rerouting the helper makes every field the create functionality *derives*
  stop being fixture input, and that the base class's constants for those fields must be re-derived
  (or re-pinned to the clock the service reads) rather than left as literals.

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `PayOrderFunctionalitySagas` | `getOrderStep` | none (primary: Order) | `payOrder: getOrderStep acquires IN_PAY_ORDER semantic lock` | Yes |
| `CollectTicketFunctionalitySagas` | `getOrderStep` | none (primary: Order) | `collectTicket: getOrderStep acquires IN_COLLECT_TICKET semantic lock` | Yes |
| `UseTicketFunctionalitySagas` | `getOrderStep` | none (primary: Order) | `useTicket: getOrderStep acquires IN_USE_TICKET semantic lock` | Yes |
| `CancelOrderFunctionalitySagas` | `getOrderStep` | none (primary: Order) | `cancelOrder: getOrderStep acquires IN_CANCEL_ORDER semantic lock` | Yes |
| `DeleteOrderFunctionalitySagas` | `getOrderStep` | none (primary: Order) | `deleteOrder: getOrderStep acquires IN_DELETE_ORDER semantic lock` | Yes |

`PreserveTicketFunctionalitySagas` has no row: it creates the aggregate it writes, so per
`docs/concepts/sagas.md` § Create Functionality Sagas no step declares a semantic lock, and per
§ Compensation Test it needs no compensation test either (the create is the saga's last step).

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/rule-enforcement-patterns.md` | § P4b is defined only as "the saga passes the **same value** to **two** aggregates". The shape actually met here is one value derived once from upstream DTOs and passed into a **single** aggregate, where the guarantee comes from there being one derivation site rather than from two recipients agreeing. | Medium | Broaden § P4b to "the value reaches the aggregate from exactly one derivation site inside the saga", with the two-aggregate case as one instance of it. |
| `docs/concepts/rule-enforcement-patterns.md` / `.claude/skills/implement-aggregate/session-c.md` | Neither says whether a P4b value is derived in the `{Op}FunctionalitySagas` class or in the service method. When the derivation reads data a P3 guard in the same service method validates, deriving it in the workflow makes the guard unreachable and surfaces an NPE instead of the rule's exception. | Medium | State that a P4b value whose inputs any P3 guard validates is derived in the service method, after the guards; the workflow's job is to fetch, not to compute. |
| `docs/concepts/testing.md` § Choosing Input Values | Gives no guidance for a P3 guard that is unreachable by construction — `SEAT_NUMBER_WITHIN_CAPACITY` cannot fire while the count guard preceding it holds, so it has no violation test and no boundary pair. | Low | Add a line permitting a documented unreachable guard with no test, provided the retro records why. |

---

## Patterns to Capture

- **Pattern:** Derived-term construction in the service, guards first
  **Observed in:** `microservices/order/service/OrderService.java` (`preserveTicket`, `bookingTerms`)
  **Description:** A create service method receives the saga-assembled DTOs, runs every P3 guard over
  them, and only then derives the fields the aggregate freezes at construction. The derivation reads
  the same DTO members the guards just validated, so no derivation can fail on data a guard would
  have rejected. Keeping the derivation behind the guards is what makes the rule's own exception —
  rather than an NPE from an absent lookup — the one the caller sees.

- **Pattern:** Allocation as enforcement
  **Observed in:** `microservices/order/service/OrderService.java` (`lowestFreeSeatNumber`)
  **Description:** A uniqueness rule over the aggregate's own table can be enforced by an allocator
  that reads the same set the rule quantifies over and picks a value outside it, instead of by a
  guard that rejects a caller-supplied value. Both are P3 (own-table read inside
  `@Transactional(SERIALIZABLE)`); the allocator variant simply has no violation path to test, so the
  rule's coverage lives in a positive test asserting the allocated value.

- **Pattern:** Fixture constants pinned to the service's clock
  **Observed in:** `TrainticketSpockTest.groovy` (`ORDER_TRAVEL_DATE` and the constants derived from it)
  **Description:** When a create functionality stamps a timestamp from `DateHandler.now()` and an
  invariant orders it against a caller-supplied date, that date cannot be a literal — the suite
  starts failing on the day real time passes it. Pinning it relative to `DateHandler.now()` keeps the
  ordering true indefinitely, and the derived constants stay relative to it.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 24

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 24 | 1 | fixed | 845176f24 |

---

## One-Line Summary

Rerouting the `createOrder` fixture onto the real booking saga is where session `c` actually bites:
every field the create functionality derives stops being fixture input, and the harness freezes the
helper's signature without saying that its constants must follow.
