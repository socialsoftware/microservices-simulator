# Retro — 2.8.a — Tournament

**App:** quizzes-full-2
**Session:** 2.8.a (Domain Layer)
**Date:** 2026-08-08

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/Tournament.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentCreator.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentParticipant.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentParticipantDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentParticipantQuizAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentTopicDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/TournamentRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/sagas/SagaTournament.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/sagas/states/TournamentSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/sagas/factories/SagasTournamentFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/sagas/repositories/TournamentCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/TournamentServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java` (10 constants appended)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/tournament/TournamentIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (Tournament fixture constants)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (factory + custom repository beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (checkbox 2.8.a)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants → Sagas, § Factories, § Repositories, § getEventSubscriptions() | Yes | R6 no-repository-read rule directly settled the placement of TOPIC_COURSE_EXECUTION as P1 on the two cached `courseAggregateId` fields |
| `docs/concepts/testing.md` | § T1 — Aggregate Test, § Choosing Input Values — EP & BVA, § Spec-First Ordering, § Directory Layout | Yes | The ordered-vs-categorical decision rule cleanly partitioned the ten predicates into four straddle pairs and six single-representative cases |
| `.claude/skills/implement-aggregate/session-a.md` | all sections | Partial | See Documentation Gaps: the Dto section does not say how an owned sub-entity that has no Dto of its own is represented |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `AGENTS.md` | § Harness evolution | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- § "Owned entity classes" → "Nested entity-to-entity `@OneToOne`" covered `TournamentParticipant → TournamentParticipantQuizAnswer` exactly, including the deep-copy requirement in the outer entity's copy constructor. No inference needed for the hardest JPA shape in this aggregate.
- § "`{Aggregate}SagaState.java`" instructing transcription of plan.md's `**Saga states:**` line verbatim removed all guesswork about states belonging to sagas that sessions 2.8.c has not written yet.
- § "Domain sentinel constants" correctly routed `ANONYMOUS` as a *consumer* import from `QuizzesFull2DomainConstants` rather than a re-declaration, which is what `CREATOR_IS_NOT_ANONYMOUS` needed.
- The mandated `mappedBy`/back-reference wiring rule produced three working bidirectional single snapshots (`execution`, `creator`, `quiz`) on the first build, with no EntityManagerFactory init failures.

### What was unclear or missing

- session-a.md says setters stamp the technical `lastModifiedTime` field but does not say what the **creating** constructor should seed it with. `Quiz` (2.6.a) could pass its `creationDate` parameter; `Tournament` has no such domain field. Resolved from the domain model's §1 technical-field note ("stamped at mutation time") by stamping `DateHandler.now()` in the constructor — creation is the first mutation, and the invariant that reads it returns early while `prev == null`, so nothing is constrained on the first version.
- § "`{Aggregate}Dto.java`" gives no rule for an owned sub-entity that the snapshot decision table gives no Dto of its own. Settled from plan.md § "Snapshot class decisions", which lists `TournamentParticipantQuizAnswer.java` with no Dto: its six fields are flattened onto `TournamentParticipantDto`.

### Suggested wording / structure changes

- `session-a.md` § "`{Aggregate}Dto.java`": add one line — an owned sub-entity with no Dto of its own is flattened into the Dto of its owning entity, never given an ad-hoc Dto.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | § "`{Aggregate}Dto.java`" is silent on how an owned sub-entity carrying no Dto of its own reaches the transport shape | Low — plan.md's snapshot decision table settles it per application, so no session is blocked | Add the flattening rule to the Dto section |
| `.claude/skills/implement-aggregate/session-a.md` | The creating constructor's seed value for a technical `lastModifiedTime`-style field is unstated; only the setter-stamping behaviour is described | Low — the domain-model technical-field note covers it | State that the creating constructor stamps the field like any other mutation |

---

## Patterns to Capture

- **Pattern:** Membership comparison for collection freezes
  **Observed in:** `microservices/tournament/aggregate/Tournament.java` (`scheduleUnchangedFrom`, `participantAggregateIds`)
  **Description:** When a temporal or state freeze names a snapshot collection among its frozen fields, compare the set of upstream aggregate ids, not the entities. The cached name/version fields on each element are refreshed by event handlers that must keep working after the freeze engages; comparing full equality would make every `UpdateTopicEvent` or `UpdateStudentNameEvent` refresh throw. Same shape as `Quiz.questionAggregateIds()` in 2.6.a — two aggregates independently arriving at it suggests it belongs in `docs/concepts/aggregate.md`.

- **Pattern:** Two freezes sharing one predicate helper
  **Observed in:** `microservices/tournament/aggregate/Tournament.java` (`fieldsFinalAfterStart`, `cancelledTournamentIsFrozen`)
  **Description:** Where two P1 rules freeze overlapping field sets under different antecedents (one temporal, one state-based), factor the common comparison into one private helper and let each rule add its own extra field and throw its own message constant. Keeps the two error messages distinguishable while the frozen-field list has exactly one definition.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

Neither gap above was friction that blocked the session: both were settled by the application's own
spec pair (plan.md § "Snapshot class decisions" and the domain model's §1 technical-field note)
rather than by a harness reading, so no gate fired. They are recorded under Documentation Gaps as
candidates for a later harness improvement.

---

## One-Line Summary

Tournament's domain layer landed with all ten P1 predicates and 26 T1 cases green on the first build; the only two skill gaps found (Dto flattening for a Dto-less owned sub-entity, and the creating constructor's `lastModifiedTime` seed) were both answerable from the application's spec pair, so no harness gate fired.
