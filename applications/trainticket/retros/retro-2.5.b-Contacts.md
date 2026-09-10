# Retro — 2.5.b — Contacts

**App:** trainticket
**Session:** 2.5.b (Read Functionalities)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/ServiceMapping.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/contacts/GetContactsByIdCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/commands/contacts/GetContactsByAccountCommand.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/ContactsRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/ContactsCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/sagas/repositories/ContactsCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/service/ContactsService.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/messaging/ContactsCommandHandler.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/sagas/GetContactsByIdFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/sagas/GetContactsByAccountFunctionalitySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/coordination/functionalities/ContactsFunctionalities.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/contacts/ContactsServiceTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/contacts/GetContactsByIdTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/coordination/contacts/GetContactsByAccountTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md`

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/architecture.md` | § Package Structure Convention | Yes | — |
| `docs/concepts/service.md` | § Injected Dependencies, § Method Patterns → Read method, § Custom Repository — Latest-Active-Version Query | Yes | The three-file table (JPA repo / abstract custom repo / sagas impl) removed all guesswork for the by-account query. |
| `docs/concepts/commands.md` | § What a Command Is, § Naming Conventions, § ServiceMapping Enum, § Routing Commands | Partial | The `rootAggregateId` guidance covers own-id, create (`null`) and unfiltered-read (`null`) shapes, but not a filtered collection read whose only id is a *foreign* aggregate's; see Documentation Gaps. |
| `docs/concepts/sagas.md` | § Read Functionality Sagas, § List-return read variant, § Two-step read saga variant | Yes | The one-step vs two-step decision was settled by the filter field living on the aggregate. |
| `docs/concepts/testing.md` | § Test Taxonomy, § Assertion Ownership, § Fake/Wrong/Weak, § Directory Layout, § T2 (incl. § Not-Found Paths), § T4 | Yes | § "Collection reads have neither path" in `session-b.md` plus § Not-Found Paths settled the `getContactsByAccount` empty-result case. |

---

## Skill Instructions Feedback

### What worked well

- `session-b.md` § "Produce" stating plan.md is a blueprint rather than a manifest: the three repository files this session needed were absent from the 2.5.b row, and the amend rule made producing them a routine step instead of a judgment call.
- The fixture-helper contract in § "Update `{AppClass}SpockTest.groovy`" — including the note added by row 18 about foreign ids — was directly applicable in the negative here: plan.md § "Cross-aggregate prerequisites" states explicitly that `CreateContacts` fetches nothing, so `userAggregateId` stays a plain literal default and 2.5.c's reroute onto the create saga will not break the call sites.
- The bean-method split (constructor args vs `@Autowired` fields) was exact; the service compiled against it first time.

### What was unclear or missing

- Nothing that blocked the session. The single soft spot was the `rootAggregateId` value for `GetContactsByAccountCommand`; see Documentation Gaps.

### Suggested wording / structure changes

- none

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/commands.md` | § "What a Command Is" enumerates two shapes with no root aggregate (create, unfiltered collection read) but is silent on the **filtered** collection read, whose only available id belongs to a *foreign* aggregate — neither "the aggregate whose semantic lock lifecycle this command participates in" nor `null` is obviously right. | Low | Add a sentence: for a filtered collection read, the foreign filter id may be passed as `rootAggregateId` since a step declaring no lock and no forbidden states never dereferences it; `null` is equally safe. Either way, name the choice so it is not re-derived per aggregate. |

---

## Patterns to Capture

- **Pattern:** Filtered collection read whose filter is a foreign id stored on the aggregate
  **Observed in:** `microservices/contacts/coordination/sagas/GetContactsByAccountFunctionalitySagas.java`
  **Description:** When the filter parameter names another aggregate but is stored as a plain column on this one, the read is a **one-step** list-return saga, not the two-step variant — no resolution step is needed because no field translation happens. `docs/concepts/sagas.md` states the rule; the ambiguity is that the parameter *looks* like the two-step trigger. Worth one clarifying clause in the one-step/two-step decision box of `session-b.md`.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: 20

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 20 | 2 | deferred | - |

---

## One-Line Summary

Contacts read side landed without harness friction; the only unsettled question was which id a filtered collection read should pass as its command's `rootAggregateId`, which the docs do not name for that shape.
