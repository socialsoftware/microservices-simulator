# Retro — 2.3.a — Topic

**App:** quizzes-full-2
**Session:** 2.3.a (Domain Layer)
**Date:** 2026-08-04

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/Topic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/TopicFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/TopicCustomRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/TopicRepository.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/TopicDto.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/sagas/SagaTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/sagas/states/TopicSagaState.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/sagas/factories/SagasTopicFactory.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/aggregate/sagas/repositories/TopicCustomRepositorySagas.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/topic/TopicServiceApplication.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/topic/TopicIntraInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/QuizzesFull2SpockTest.groovy` (appended: `TOPIC_AGGREGATE_ID`, `TOPIC_NAME`)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (appended: `sagasTopicFactory`, `topicCustomRepositorySagas` beans + imports)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md` (manager: 2.3.a row amended for the two shared-test-file appends; 2.3.a checkbox ticked)

### Application bug fixes (earlier-session files)

- (none)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Application isolation, § Harness log, § Run the test suite | Yes | The "report per-class, say which total you are quoting" paragraph did what it promises — the unfiltered 51 would otherwise have been reported as the slice's own. |
| `.claude/skills/implement-aggregate/session-a.md` | § Reads, § Verify Mandatory Files, § Produce (all subheadings), § Update `{AppClass}SpockTest.groovy`, § Update `BeanConfigurationSagas.groovy` | Yes | Both mandatory interface files were already in the 2.3.a plan row, so no amendment was needed there. |
| `docs/concepts/aggregate.md` | § Key Fields, § Base Class, § Variants → Sagas, § Factories, § Repositories, § Naming Conventions | Yes | `getEventSubscriptions()` correctly skipped: Topic subscribes to nothing. |
| `docs/concepts/testing.md` | § T1, § Choosing Input Values (EP & BVA), § Spec-First Ordering, § Directory Layout | Yes | § T1's "final fields need no coverage in any tier" is what makes a one-case T1 correct here rather than thin. |
| `applications/quizzes-full-2/plan.md` | § 3 Topic, snapshot-class decisions table, § 3.1 / § 3.2 rule tables, path conventions | Yes | The snapshot row `Topic | Course | single, n/a | none - courseAggregateId field on Topic` settled the field shape without ambiguity. |
| `applications/quizzes-full-2/quizzes-full-2-domain-model.md` | § 1 entity table, § 2 relationships | Yes | § 2's "Topic → Course, immutable: yes" is what makes `courseAggregateId` a `final` field. |

---

## Skill Instructions Feedback

### What worked well

- (2.3.a1) session-a.md's per-file subheadings are specific enough that an aggregate this small needed no judgment calls.
- (2.3.a1) The `SagaTopic` copy-constructor rationale (inherit `sagaState`, never reset) is stated with its failure mode, so it is followed rather than guessed at.

### What was unclear or missing

- (2.3.a1) Nothing blocking. Minor: session-a.md § Produce says "Produce every file listed in the plan.md `2.{N}.a` row", and separately mandates the two shared-test-file updates. A slice agent reading the row as a manifest would silently skip both. The "blueprint, not a manifest" blockquote covers this, but the two mandates live several sections away from the row that omits them.

### Suggested wording / structure changes

- (2.3.a1) Consider stating once in session-a.md that an aggregate with zero P1 rules is a correct shape, rather than leaving it to be re-derived per aggregate.

### Manager observations

- One slice, `2.3.a1`, covering the whole session. Topic's item counts are at or below the slicing threshold, so plan.md carries no sub-checkboxes for 2.3.a and the implicit single-slice path applied.
- Zero re-spawns. The slice returned `STATUS: DONE` with `FRICTION: none`.
- `git status --porcelain` after the slice confirmed every changed path was under `applications/quizzes-full-2/`. No contract breach.
- The session-end full clean suite found no cross-slice regression: `MAVEN_EXIT=0`, `tests=51 failures=0 errors=0 skipped=0`. Nothing was invisible to the slice's narrow run, as expected for a single-slice session.
- No Type 2 or `2-fw` escalation was raised, so no question went to the human.
- The slice's only reported gap targets `applications/quizzes-full-2/plan.md`, which is an application artifact rather than a harness artifact. Per `conventions.md` § "Harness log" it is not harness friction; it was handled as a plan.md amendment under § "Amend plan.md for omitted files".

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-a.md` | The two mandated shared-test-file updates sit several sections away from § Produce's "produce every file listed in the plan.md row", so a row that omits them reads as complete | Low - reported by 2.3.a1; both files were produced anyway | Cross-reference the two update sections from § Produce, or restate there that the plan row is never the full set |

---

## Patterns to Capture

- **Pattern:** Zero-P1 aggregate shape
  **Observed in:** `microservices/topic/aggregate/Topic.java`, `sagas/topic/TopicIntraInvariantTest.groovy`
  **Description:** An aggregate with no P1 rules is a real and correct shape. `verifyInvariants()` gets an empty body with a comment naming why it is empty (which spec sections were checked), and T1 gets the happy path alone plus a comment stating the same. `Course.java` set the precedent inside this app and `Topic` follows it.

- **Pattern:** Provenance-annotated shared-file appends in plan.md rows
  **Observed in:** `applications/quizzes-full-2/plan.md` rows 2.1.a, 2.2.a, now 2.3.a
  **Description:** Annotating each shared-file append with the mandating skill section — `(appended 2.N.a - session-a.md § ... mandates ...)` — is what makes an omission in a later row visible. Worth treating as the standard for every session row rather than an ad-hoc note.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: none

For each `fixed` row, the `harness:` commit sha:

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| (none) | - | - | - |

---

## One-Line Summary

Topic's domain layer went in clean on the first slice with zero harness friction, confirming that a zero-P1 aggregate is a well-supported shape in the current harness.
