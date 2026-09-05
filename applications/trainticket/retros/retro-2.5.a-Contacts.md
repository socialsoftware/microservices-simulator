# Retro — 2.5.a — Contacts

**App:** trainticket
**Session:** 2.5.a (Domain Layer)
**Date:** 2026-08-23

---

## Files Produced

### Application files (trainticket)

- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/Contacts.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/ContactsDto.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/ContactsFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/ContactsCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/ContactsRepository.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/sagas/SagaContacts.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/sagas/states/ContactsSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/sagas/factories/SagasContactsFactory.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/aggregate/sagas/repositories/ContactsCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/contacts/ContactsServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/main/java/pt/ulisboa/tecnico/socialsoftware/trainticket/microservices/exception/TrainticketErrorMessage.java` (appended: `CONTACTS_DOCUMENT_NUMBER_PRESENT`)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/sagas/contacts/ContactsIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/TrainticketSpockTest.groovy` (appended: Contacts domain constants)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/trainticket/BeanConfigurationSagas.groovy` (appended: factory and custom-repository beans)
- `/Users/frleitao/thesis/microservices-simulator-trainticket/applications/trainticket/plan.md` (checkbox ticked, 2.5.a row amended)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to repository root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite, § Neutral domain | Yes | — |
| `.claude/skills/implement-aggregate/session-a.md` | all sections | Partial | § "Domain enums" still silent on the JPA mapping of the enum-typed aggregate *field* (harness-log row 15, deferred). |
| `docs/concepts/testing.md` | § T1 — Aggregate Test | Yes | Template matched the CONTACTS_DOCUMENT_NUMBER_PRESENT implication directly. |
| `.claude/skills/_shared/session-completion.md` | all sections | Yes | — |

**Sufficient?** = `Yes` / `Partial` / `No`

---

## Skill Instructions Feedback

### What worked well

- § "Domain enums" `(shared)` rule plus plan.md's explicit note kept `DocumentType` a single type: the
  session imported `{src}enums/DocumentType.java` rather than emitting a second copy in
  `contacts/aggregate/`, which would have broken session 2.8.c's Order snapshot fields.
- § `{Aggregate}SagaState.java` "Include `READ_{AGGREGATE}` if other aggregates use this aggregate as
  a cross-aggregate prerequisite" resolved without inference: plan.md's Order section lists
  CONTACTS_EXIST as a P4a fetch, so `READ_CONTACTS` was required.
- § `{Aggregate}CustomRepository.java` explicitly permitting an empty interface body removed the
  temptation to speculate a `findAllLatestActive` before session `b` establishes it is needed -
  Contacts has no own-table uniqueness rule, so it may never need one.

### What was unclear or missing

- Nothing new. The only gap hit was the already-logged row 15 (enum field mapping), whose stated
  reading was followed.

### Suggested wording / structure changes

- none

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | § "Domain enums" does not state how the aggregate field typed by a domain enum is mapped; both `@Enumerated(EnumType.STRING)` and JPA's `ORDINAL` default compile and persist, so the build cannot catch the wrong choice. Already open as harness-log row 15. | Medium - recurs for Trip, PriceConfig and Order | Add one line to § "Domain enums": an aggregate field typed by a domain enum carries `@Enumerated(EnumType.STRING)`, so stored rows survive a reordering of the enum constants. |

---

## Patterns to Capture

- **Pattern:** Immutable single-reference snapshot id as a `final` field
  **Observed in:** `microservices/contacts/aggregate/Contacts.java` (`userAggregateId`)
  **Description:** Where grouping §2 marks a single-reference cached id immutable, the aggregate holds
  it as a `final` field seeded in both the creating and the copy constructor, with a getter and no
  setter, and no `verifyInvariants()` check - the compiler is the enforcement. The `Dto`, by contrast,
  keeps a mutable setter, since it is the transport the create functionality populates. This is the
  same shape session-a.md prescribes for a `final` P1 attribute, applied to a reference rather than
  to an attribute, and it is currently only derivable by analogy.

---

## Harness Changes

Rows appended to `applications/trainticket/harness-log.md` this session: none

The one gap encountered (the enum-field JPA mapping) is the recurrence that open row 15 explicitly
predicted for Contacts; it is not a distinct friction point and was handled on row 15's stated
reading.

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | - | - | - |

---

## One-Line Summary

Contacts' domain layer is User's shape plus one immutable snapshot reference, and the session
produced it with no new harness friction - the only gap hit was the enum-field mapping already open
as row 15.
