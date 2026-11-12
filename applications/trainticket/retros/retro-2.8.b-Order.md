# Retro — 2.8.b — Order

**App:** trainticket
**Session:** 2.8.b (Read Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/sagas/repositories/OrderCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/service/OrderService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/messaging/OrderCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/GetOrderByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/GetOrdersCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/GetOrdersByAccountCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/order/GetLeftTicketCountCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/GetOrderByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/GetOrdersFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/GetOrdersByAccountFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/GetLeftTicketCountFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/functionalities/OrderFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/order/OrderServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/GetOrderByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/GetOrdersTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/GetOrdersByAccountTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/order/GetLeftTicketCountTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | The "never pipe maven" rule was needed: the rtk hook truncated the first build's output. |
| `.claude/skills/implement-aggregate/session-b.md` | all | Partial | See § Skill Instructions Feedback - the fixture-helper section covers a single foreign prerequisite, not a chain of mutually consistent ones. |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository — Latest-Active-Version Query | Yes | The three-file rule (JPA repo / abstract custom repo / sagas custom repo) applied verbatim to all three Order queries. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Unfiltered variant, § Two-step read saga variant | Partial | The two-step variant resolves a foreign id to a *filter field*; GetLeftTicketCount chains two foreign fetches to obtain a *scalar the primary read consumes*. Generalised, but not the shape written down. |
| `docs/concepts/commands.md` | § What a Command Is (null `rootAggregateId`), § Naming Conventions, § Routing Commands | Yes | Row 22's broadened enumeration already covered `GetOrdersByAccountCommand` and `GetLeftTicketCountCommand` passing `null`. |
| `docs/concepts/testing.md` | § T2 — Service Test → Not-Found Paths, § T4 — Functionality Test, § Assertion Ownership | Yes | Path A for `getOrderById`; the "collection reads have neither path" rule settled the other three. |
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | The JDK-21 note was needed - the shell defaults to 17. |

**Sufficient?** = `Yes` / `Partial` / `No`

---

## Skill Instructions Feedback

### What worked well

- § "Prerequisite — ServiceMapping" and its multi-word naming note: `ORDER("order")` was unambiguous, and the bean-name rule made `orderCommandHandler` fall out.
- The bean-config section's explicit split between constructor parameters and `@Autowired` fields produced a compiling `OrderService` bean on the first attempt.
- § "Deferred reads" was checked and found not to apply - Order is last in the topological order, so all four reads were implementable.

### What was unclear or missing

- § "Update `{AppClass}SpockTest.groovy`" → "Foreign aggregate ids are never defaulted to a constant" tells the helper to mint each prerequisite by calling the foreign `create{Foreign}(...)` helper. Order's booking saga needs prerequisites that must be *mutually consistent*, not merely present: the Trip's route has to carry the endpoint station names the order names, and a PriceConfig has to exist for that exact (route, trainType) pair. No single existing helper produces that, so `createBookableTrip(...)` was added as an intermediate fixture and used as `createOrder`'s default. The skill does not name this case.
- The same section says defaults should be "the domain constant", but three of Order's construction fields (`contactsName`, `contactsDocumentType`, `contactsDocumentNumber`) are copies the booking saga takes from the fetched `ContactsDto`. Defaulting them to constants would let the fixture and the 2.8.c saga disagree silently, so they are read back from `contactsFunctionalities.getContactsById(...)` instead. Not covered.
- `docs/concepts/sagas.md` § "Two-step read saga variant" is written for resolving a foreign id into the primary aggregate's own filter field. `GetLeftTicketCount` is a three-step chain whose middle step exists only to turn a foreign id into a **scalar the primary read consumes** (the seat capacity), which is the P4b shape from plan.md § "Cross-aggregate prerequisites" rather than a filter resolution. Both the step-chaining mechanics and the P4b comment were carried over from the write-saga sections.

### Suggested wording / structure changes

- `.claude/skills/implement-aggregate/session-b.md`, § "Update `{AppClass}SpockTest.groovy`": extend the "Foreign aggregate ids are never defaulted to a constant" block to the transitive case - when the create saga's prerequisites must agree with each other, mint them together in an intermediate `create{Scenario}()` helper and default the parameter to that, and read any field the saga *copies* from a fetched DTO back from that DTO rather than from a constant.
- `docs/concepts/sagas.md`, § "Read Functionality Sagas": add a "scalar-supplying read chain" subsection beside § "Two-step read saga variant" - a read whose primary command needs a value the aggregate does not store gets one data-assembly step per hop, and the derived value is passed as a single scalar with a `[P4b]` comment.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-b.md` | § "Update `{AppClass}SpockTest.groovy`" covers minting one foreign prerequisite, not a set that must be mutually consistent, nor fields the create saga copies out of a fetched DTO. | Medium | Extend the block per § Suggested wording changes above. |
| `docs/concepts/sagas.md` | § "Read Functionality Sagas" has no shape for a read whose primary command needs a scalar derived from a chain of upstream fetches (P4b on the read side). | Medium | Add a "scalar-supplying read chain" subsection. |

---

## Patterns to Capture

- **Pattern:** Scenario fixture for mutually consistent prerequisites
  **Observed in:** `applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (`createBookableTrip`)
  **Description:** When a create saga's data-assembly steps must agree with one another (an id resolving to an aggregate whose *contents* satisfy a later guard), a single `create{Foreign}()` call per parameter is not enough. Build one intermediate helper that mints the whole consistent set and returns the id the saga takes, and default the create helper's parameter to it. The helper stays overridable so a test can deliberately supply an inconsistent set.

- **Pattern:** Shared latest-active set behind several rules
  **Observed in:** `applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/aggregate/OrderCustomRepository.java` (`findAllLatestActiveHoldingSeatOn`)
  **Description:** Where a read functionality, a saga's allocation step and two P3 guards all range over the same filtered latest-active set, declare it once on the custom repository with the exclusion baked in (here: a cancelled order releases its seat) rather than filtering at each call site. Session `b` writes it because the read needs it first; session `c` reuses it.

- **Pattern:** Derived scalar crosses the aggregate boundary, not the DTO
  **Observed in:** `applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/order/coordination/sagas/GetLeftTicketCountFunctionalitySagas.java`
  **Description:** The saga fetches the upstream DTOs, reduces them to the one scalar the primary service method needs (`capacityOf(trainTypeDto, seatClass)`), and passes that. The primary service keeps no field, no import and no knowledge of the upstream aggregate - the read-side counterpart of the P4b write pattern.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

No gate fired. The two gaps above are extensions of patterns the harness does state, not
contradictions of them, and neither required a design decision that changed the code - so they are
recorded here as documentation gaps rather than as Type 1 fixes or Type 2 halts.

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

---

## One-Line Summary

The four Order reads landed on documented patterns, but both frictions came from the same blind spot:
the harness describes single-hop cross-aggregate dependencies, and Order's are chains - a fixture
whose prerequisites must agree with each other, and a read whose capacity scalar takes two hops to
reach.
