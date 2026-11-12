# Retro — 2.5.a — Question

**App:** quizzes-full-2
**Session:** 2.5.a (Domain Layer)
**Date:** 2026-08-06

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/Question.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/Option.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/OptionDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionTopicDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/QuestionRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/sagas/SagaQuestion.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/sagas/states/QuestionSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/sagas/factories/SagasQuestionFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/aggregate/sagas/repositories/QuestionCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/question/QuestionServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/exception/QuizzesFull2ErrorMessage.java` (appended `TOPIC_BELONGS_TO_QUESTION_COURSE`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/question/QuestionIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (Question constants + `COURSE_AGGREGATE_ID_2`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (two Question beans + imports)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (ticked 2.5.a)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `AGENTS.md` | § Harness evolution, § Build Commands | Yes | — |
| `.claude/skills/_shared/conventions.md` | § Anchor to repo root, § Resolve app context, § Application isolation, § Harness log, § Run the test suite | Yes | — |
| `.claude/skills/implement-aggregate/session-a.md` | all sections | Yes | — |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants, § Factories, § Repositories, § getEventSubscriptions() | Yes | Subscriptions section skipped by design — session `a` emits an empty set |
| `docs/concepts/testing.md` | § T1, § Choosing Input Values (EP & BVA), § Fake/Wrong/Weak, § Spec-First Ordering | Yes | § Choosing Input Values settled that `TOPIC_BELONGS_TO_QUESTION_COURSE` is categorical and needs no boundary pair |
| `applications/quizzes-full-2/plan.md` | §5 Question, § Rule Classification §3.1/§3.2, § Snapshot class decisions | Yes | — |
| `applications/quizzes-full-2/quizzes-full-2-domain-model.md` | §1 entity table, §2 relationships | Yes | — |
| `applications/quizzes-full-2/quizzes-full-2-aggregate-grouping.md` | §2 snapshots | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The explicit `@OneToMany(cascade = ALL, orphanRemoval = true)` **without** `mappedBy` instruction covered both owned collections (`Option`, `QuestionTopic`) with no ambiguity.
- The `Saga{Aggregate}` copy-constructor rule (inherit `other.getSagaState()`, never reset) and the `@Convert(converter = SagaStateConverter.class)` note removed the two failure modes that would only surface much later.
- "Transcribe the `**Saga states:**` line; do not derive" produced `QuestionSagaState` directly from plan.md with no guesswork.
- The mandatory-files check (`{Aggregate}Factory`, `{Aggregate}CustomRepository`) was already satisfied by plan.md's 2.5.a row — nothing to amend.

### What was unclear or missing

- Session `a` does not state whether an **owned collection** belongs in the creating constructor's parameter list. It is resolvable — the "collection field additionally gets `add{Element}` / `remove{Element}`" rule implies collections are populated after construction — but the inference is one step removed from the text. `Option` makes this sharper than a snapshot collection does: a question without options is not a meaningful domain object, yet the constructor cannot take them under the current reading.

### Suggested wording / structure changes

- In `session-a.md` § `{Aggregate}.java`, extend the "Constructor" bullet to say explicitly that owned-entity collections are **not** constructor parameters — they start empty and are populated by the creating service through the `add{Element}` helpers, so the same code path serves creation and later mutation.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | § `{Aggregate}.java` does not say whether owned-entity collections are constructor parameters | Low | Add the explicit statement proposed under § Skill Instructions Feedback |

---

## Patterns to Capture

- **Pattern:** Domain-model silence on immutability is a positive signal
  **Observed in:** `applications/quizzes-full-2/src/main/java/.../question/aggregate/Question.java`
  **Description:** The domain model marks immutable attributes explicitly (`(immutable)` in §1, `Immutable = yes` in §2). `Question.creationDate` carries neither marking and no §3.1 `*_FINAL` rule, so it is a plain mutable field with a setter, while `courseAggregateId` — marked immutable in §2 — is a Java `final` field with no setter. Reading the absence of a marking as "mutable" rather than as "unspecified" is what keeps `final` from over-enforcing and silently excluding a field from every test tier.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| — | — | — | — |

Rows whose outcome is `declined` or `deferred` have no sha - write `-`.

---

## One-Line Summary

The Question domain layer landed with no harness friction; the only soft spot was session `a` leaving the owned-collection-in-constructor question to inference, which `Option` makes more visible than a snapshot collection would.
