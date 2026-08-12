# Retro — 2.3.a — Topic

**App:** quizzes-full
**Session:** 2.3.a (Domain Layer)
**Date:** 2026-05-01

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/Topic.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/TopicCourse.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/TopicFactory.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/TopicCustomRepository.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/TopicDto.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/TopicRepository.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/sagas/SagaTopic.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/sagas/states/TopicSagaState.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/sagas/factories/SagasTopicFactory.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/aggregate/sagas/repositories/TopicCustomRepositorySagas.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/topic/TopicServiceApplication.java`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/topic/TopicTest.groovy`
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/BeanConfigurationSagas.groovy` (modified)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/aggregate.md` | § Key Fields, § Sagas variant, § Factories, § Repositories, § Naming Conventions, § getEventSubscriptions() | Yes | — |
| `docs/concepts/testing.md` | § T1 — Creation Test | Yes | — |

---

## Reference App Consulted

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/src/main/java/.../topic/aggregate/Topic.java` | Confirmed JPA mapping for `topicCourse` (`@OneToOne(cascade=ALL, mappedBy="topic")`) | session-a.md doesn't explain owned-entity JPA relationship direction |
| `applications/quizzes/src/main/java/.../topic/aggregate/TopicCourse.java` | Confirmed fields (`courseAggregateId`, `courseVersion`) and owning side of `@OneToOne` | session-a.md doesn't describe which side owns the FK in a bidirectional OneToOne |
| `applications/quizzes/src/main/java/.../topic/aggregate/sagas/SagaTopic.java` | Confirmed copy constructor pattern | — |
| `applications/quizzes/src/main/java/.../topic/aggregate/sagas/states/TopicSagaState.java` | Confirmed `READ_TOPIC` is the only saga state | — |
| `applications/quizzes/src/main/java/.../topic/aggregate/TopicFactory.java` | Confirmed interface method naming (`createTopicFromExisting`) — chose `createTopicCopy` to match quizzes-full convention instead | — |
| `applications/quizzes/src/main/java/.../topic/aggregate/sagas/factories/SagasTopicFactory.java` | Confirmed factory implementation pattern | — |
| `applications/quizzes/src/main/java/.../topic/aggregate/TopicCustomRepository.java` | Confirmed empty interface body (no cross-table lookups for Topic) | — |
| `applications/quizzes/src/main/java/.../topic/aggregate/sagas/repositories/TopicCustomRepositorySagas.java` | Confirmed autowired repository pattern | — |
| `applications/quizzes/src/main/java/.../topic/aggregate/TopicDto.java` | Confirmed DTO fields and constructor | — |
| `applications/quizzes/src/main/java/.../topic/aggregate/TopicRepository.java` | Confirmed plain `JpaRepository<Topic, Integer>` (no custom queries) | — |
| `applications/quizzes/src/main/java/.../topic/TopicServiceApplication.java` | Confirmed `@EntityScan` doesn't need a quizzes-full events package (unlike quizzes which has `quizzes.events`) | session-a.md doesn't explain when/whether an events subpackage appears in EntityScan |

---

## Skill Instructions Feedback

### What worked well

- The file list in session-a.md was comprehensive and covered every file class well.
- Owned entity class description clearly explains `@OneToMany` cascade/orphanRemoval — though Topic uses `@OneToOne` rather than a collection.

### What was unclear or missing

- session-a.md mentions `@OneToMany` for owned entity collections but Topic uses a `@OneToOne` owned entity. The doc doesn't cover the `@OneToOne` case with `mappedBy`.
- plan.md 2.3.a row omitted `TopicCourse.java`, `TopicFactory.java`, and `TopicCustomRepository.java`. These are always required by session-a but were systematically missing from plan.md. This is a recurring gap (same happened for Course and User).

### Suggested wording / structure changes

- `session-a.md` § Owned entity classes: add a note covering `@OneToOne(mappedBy="...")` as the inverse side when the owned entity holds the FK.
- `classify-and-plan` skill: always include `{Aggregate}Factory.java`, `{Aggregate}CustomRepository.java`, and any owned entity classes in the 2.N.a row automatically.

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `session-a.md` | `@OneToOne` owned-entity JPA relationship direction not covered; only `@OneToMany` is described | Medium | Add example of inverse `@OneToOne(mappedBy=...)` on the aggregate side |
| `session-a.md` (plan.md generation) | `TopicFactory`, `TopicCustomRepository`, and owned entities consistently missing from plan.md 2.N.a row | High | The classify-and-plan skill should always add these three categories to 2.N.a |

---

## Patterns to Capture

- **Pattern:** `TopicCourse` bidirectional `@OneToOne` — aggregate holds inverse side (`mappedBy="topic"`); entity holds FK with plain `@OneToOne`.
  **Observed in:** `Topic.java`, `TopicCourse.java`
  **Description:** When an aggregate owns a single sub-entity (rather than a collection), use `@OneToOne(cascade=ALL, mappedBy="entity_field")` on the aggregate side and plain `@OneToOne` on the entity side. `setTopicCourse()` must call `topicCourse.setTopic(this)` to wire the back-reference.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/classify-and-plan` | Always include factory interface, custom repository interface, and any owned entity classes in the 2.N.a file row |
| Medium | `.claude/skills/implement-aggregate/session-a.md` | Add `@OneToOne(mappedBy=...)` owned entity pattern alongside the existing `@OneToMany` pattern |

---

## One-Line Summary

The `TopicCourse` owned entity uses a bidirectional `@OneToOne` (not `@OneToMany`) that requires `mappedBy` on the aggregate side — a pattern absent from session-a.md that was resolved by consulting the reference app.
