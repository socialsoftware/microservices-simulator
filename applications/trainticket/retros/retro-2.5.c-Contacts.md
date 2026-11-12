# Retro — 2.5.c — Contacts

**App:** trainticket
**Session:** 2.5.c (Write Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/contacts/CreateContactsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/contacts/UpdateContactsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/contacts/DeleteContactsCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/service/ContactsService.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/messaging/ContactsCommandHandler.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/sagas/CreateContactsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/sagas/UpdateContactsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/sagas/DeleteContactsFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/functionalities/ContactsFunctionalities.java` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/webapi/ContactsController.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (amended: `createContacts` rerouted onto CreateContacts, second-value literals added)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/contacts/ContactsServiceTest.groovy` (appended)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/contacts/CreateContactsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/contacts/UpdateContactsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/contacts/DeleteContactsTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/contacts/UpdateContactsCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/contacts/DeleteContactsCompensationTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/UpdateContactsCompensationTest/UpdateContactsFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/resources/groovy/DeleteContactsCompensationTest/DeleteContactsFunctionalitySagas.csv`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (checkbox ticked, 2.5.c row amended)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/service.md` | § Method Patterns (Create / Mutate), § Copy-on-Write Rule, § DTO Immutability, § Exception-Throw Convention, § P3 Guard Placement | Yes | Contacts has no P3 guard; the create/mutate/soft-delete shapes transferred without inference |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § File Location, § ServiceMapping Enum, § Sending Commands, § Routing Commands | Yes | `rootAggregateId = null` for the create command is stated explicitly |
| `docs/concepts/sagas.md` | § Step Ordering, § Lock-Acquisition Step Pattern, § R4 Decision Table, § Create Functionality Sagas (Shape 1), § Semantic-lock release on abort is automatic, § Write Workflow Structure | Yes | CreateContacts is Shape 1 (single-step, no compensation registered) |
| `docs/concepts/testing.md` | § Assertion Ownership, § T2 — Service Test, § T4 — Functionality Test, § Soft-delete functionalities, § Compensation Test, § CRITICAL gotcha, § Fake/Wrong/Weak checklist | Yes | The soft-delete happy-path carve-out and the one-file-per-compensation-test rule both applied directly |
| `.claude/skills/implement-aggregate/session-c.md` | whole file | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Application isolation, § Harness log, § Run the test suite (incl. § Inspecting maven output) | Yes | The subprocess capture recipe confirmed both lock steps ran before their injected fault |

---

## Skill Instructions Feedback

### What worked well

- § "Update `{AppClass}SpockTest.groovy`" is unambiguous about keeping the 2.5.b signature and defaults; `createContacts` rerouted onto `contactsFunctionalities.createContacts` with no call-site churn, and 2.5.b's four read tests passed unchanged against the real create path.
- The § Compensation Test one-file-per-test rule plus the CSV naming rule (`{Op}FunctionalitySagas.csv` under `{TestClass}/`) produced working fault injection first try. The fixture's own `CreateContactsFunctionalitySagas` instantiation in `setup()` does not disturb the block index, since the index is per saga class.

### What was unclear or missing

- (none this session)

### Suggested wording / structure changes

- (none)

---

## Semantic-Lock Coverage Audit

| Saga class | Step name | Foreign aggregate locked | Lock-acquisition test name | Present? |
|------------|-----------|--------------------------|----------------------------|---------:|
| `CreateContactsFunctionalitySagas` | — (no `setSemanticLock`; create step per sagas.md § Create Functionality Sagas) | — | n/a | n/a |
| `UpdateContactsFunctionalitySagas` | `getContactsStep` | none (primary aggregate, `IN_UPDATE_CONTACTS`) | `updateContacts: getContactsStep acquires IN_UPDATE_CONTACTS semantic lock` (`UpdateContactsTest`) | Yes |
| `DeleteContactsFunctionalitySagas` | `getContactsStep` | none (primary aggregate, `IN_DELETE_CONTACTS`) | `deleteContacts: getContactsStep acquires IN_DELETE_CONTACTS semantic lock` (`DeleteContactsTest`) | Yes |

Compensate transitions: `UpdateContactsCompensationTest` and `DeleteContactsCompensationTest`, both
with the lock step genuinely executed before the injected fault (verified from the
`START EXECUTION STEP: getContactsStep` console lines via the subprocess capture recipe).

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| (none) | — | — | — |

---

## Patterns to Capture

- **Pattern:** Write session for an aggregate with no cross-aggregate prerequisites and no P3 rules
  **Observed in:** `microservices/contacts/coordination/sagas/`
  **Description:** All three sagas reduce to the doc templates verbatim - Shape 1 create, and the two-step get-then-lock for update and delete. No data-assembly step, no `setForbiddenStates`, no service guard, and the only T2 violation cases are the Path A not-found ones. Worth noting as the baseline shape a session `c` collapses to when plan.md lists no prerequisites, so the absence of guard tests is not read as missing coverage.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

---

## One-Line Summary

The first write session that hit no harness friction at all: with no cross-aggregate prerequisites and no P3 rules, Contacts' three write functionalities fell straight out of the sagas and testing templates, including both compensation tests.
