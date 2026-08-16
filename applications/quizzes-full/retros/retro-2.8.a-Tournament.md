# Retro — 2.8.a — Tournament

**App:** quizzes-full
**Session:** 2.8.a (Domain Layer)
**Date:** 2026-05-16

---

## Files Produced

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/Tournament.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentParticipant.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentParticipantQuizAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/sagas/SagaTournament.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/sagas/states/TournamentSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/sagas/factories/SagasTournamentFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/sagas/repositories/TournamentCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/aggregate/TournamentRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/tournament/TournamentServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/tournament/TournamentTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/exception/QuizzesFullErrorMessage.java` (modified)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full/plan.md` (modified)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Variants (SagaAggregate), § Factories, § Repositories, § getEventSubscriptions() | Yes | — |
| `docs/concepts/testing.md` | § T1 — Creation Test | Yes | — |

---

## Skill Instructions Feedback

### What worked well

- The session-a.md file covers all necessary artifact types. The mandatory-file check for `TournamentFactory.java` and `TournamentCustomRepository.java` correctly flagged the two missing entries in plan.md.
- The `TournamentTopic.java` omission from plan.md was unambiguous — the aggregate caches topic snapshots and this entity is required to compile.
- Owned-entity patterns (TournamentParticipant → TournamentParticipantQuizAnswer via `@OneToOne`) were clear from existing QuestionCourse/QuestionTopic examples.

### What was unclear or missing

- session-a.md says "Always includes `NOT_IN_SAGA`" in TournamentSagaState, but all existing state enums (QuizSagaState, QuizAnswerSagaState) do NOT include it — `GenericSagaState.NOT_IN_SAGA` is used for initial state instead. The instruction is incorrect.
- session-a.md's instructions for entity-to-entity `@OneToOne` (TournamentParticipant → TournamentParticipantQuizAnswer) are not directly covered — the doc only describes aggregate-to-entity. Resolved by using a unidirectional `@OneToOne` with `cascade = CascadeType.ALL`.

### Suggested wording / structure changes

- `session-a.md` § TournamentSagaState: Remove "Always includes `NOT_IN_SAGA`" and clarify that `GenericSagaState.NOT_IN_SAGA` is used for initialization; the custom enum only includes operation-specific states.
- `session-a.md` § Owned entity classes: Add a note for entity-to-entity OneToOne (nested owned entities like TournamentParticipant → TournamentParticipantQuizAnswer), distinguishing from aggregate-to-entity.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/aggregate.md` or `session-a.md` | No guidance on SagaState enum NOT including NOT_IN_SAGA (session-a says to include it but existing code does not) | Medium | Clarify that custom SagaState enums only have operation-specific states; NOT_IN_SAGA comes from GenericSagaState |
| `.claude/skills/implement-aggregate/session-a.md` | No coverage of entity-to-entity nested OneToOne (only aggregate-to-entity) | Low | Add a note for nested entity ownership (e.g., TournamentParticipant → TournamentParticipantQuizAnswer) |

---

## Patterns to Capture

- **Pattern:** Nested owned-entity OneToOne (entity-within-entity)
  **Observed in:** `TournamentParticipant.java`
  **Description:** When an owned entity itself owns a single sub-entity, use a unidirectional `@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)` on the outer entity. No `mappedBy` is needed unless a back-reference is required. The copy constructor deep-copies the sub-entity.

- **Pattern:** Tournament has three mandatory plan.md additions — TournamentFactory, TournamentCustomRepository, TournamentTopic — all unambiguous omissions.
  **Observed in:** `plan.md` 2.8.a row
  **Description:** Any aggregate that caches N upstream snapshots of type not already listed in plan.md will require a dedicated entity class (TournamentTopic). This should be pre-planned in classify-and-plan.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-a.md` | Fix "Always includes NOT_IN_SAGA" — should say NOT_IN_SAGA is handled by GenericSagaState; custom enum only lists operation-specific states |
| Medium | `.claude/skills/implement-aggregate/session-a.md` | Add note on entity-to-entity (nested) OneToOne for owned sub-entities |
| Low | `.claude/skills/classify-and-plan.md` (or plan template) | Pre-list topic-snapshot entity (e.g., TournamentTopic) in plan.md 2.N.a row during Phase 1 |

---

## One-Line Summary

The session-a.md instruction to include `NOT_IN_SAGA` in the custom SagaState enum is wrong — it conflicts with every existing implementation; fix it.
