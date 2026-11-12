# Retro — 2.8.d — Tournament

**App:** quizzes-full-2
**Session:** 2.8.d (Event Wiring)
**Date:** 2026-08-09

---

## Files Produced

### Application files (quizzes-full-2)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesUpdateStudentName.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesAnonymizeStudent.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesDeleteUser.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesUpdateTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesDeleteTopic.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesDeleteCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesDisenrollStudentFromCourseExecution.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesInvalidateQuiz.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/subscribe/TournamentSubscribesQuizAnswerQuestionAnswer.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/handling/TournamentEventHandling.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/notification/handling/handlers/TournamentEventHandler.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/eventProcessing/TournamentEventProcessing.java`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/aggregate/Tournament.java` (getEventSubscriptions under the ACTIVE guard)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/service/TournamentService.java` (nine ByEvent mutate helpers)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/microservices/tournament/coordination/functionalities/TournamentFunctionalities.java` (nine ByEvent methods plus isInSaga)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/BeanConfigurationSagas.groovy` (three event-wiring beans)
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/tournament/TournamentInterInvariantTest.groovy`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/plan.md`
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/harness-log.md`

### Application bug fixes (earlier-session files)

- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/events/QuizAnswerQuestionAnswerEvent.java` — 2.7.c anchored the event on `quizAnswerAggregateId`, an id its only consumer can never hold. Re-anchored on `quizAggregateId` (harness-log row 71).
- `/Users/frleitao/thesis/microservices-simulator/applications/quizzes-full-2/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull2/sagas/quizanswer/QuizAnswerServiceTest.groovy` — the T2 payload case's `publisherAggregateId` assertion follows the new anchor.

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/events.md` | § Event Classes, § Publishing Events, § EventSubscription (incl. the snapshot-seeded-version subsection), § EventHandler, § Polling, § Canonical Wiring Snippet, § ByEvent sagaState guard, § Cascade Invalidation Pattern | Partial | § EventSubscription asserted `subscribesEvent()` is never called in the sagas profile; `EventService.getSubscribedEvents` applies it as a filter. Type 1, fixed (row 72). § "Always advance the cached publisher version" gives no rule for a consumer whose anchor carries **two** event types with different fold semantics — see Documentation Gaps. |
| `docs/concepts/testing.md` | § Test Taxonomy, § Assertion Ownership, § Fake/Wrong/Weak checklist, § T3 — Subscription (Inter-Invariant) Test, § Choosing Input Values | Yes | The deletion-event `and:`-block pattern and the `loadForCheck` read-back shape both transferred without adaptation. |
| `.claude/skills/implement-aggregate/session-d.md` | all sections | Partial | § Shared-anchor events covers filtering on a shared anchor but assumes the anchor is already reachable; it has no case for a declared consumer that cannot construct the anchor at all. |
| `.claude/skills/_shared/conventions.md` | § Anchor to the repository root, § Resolve app context, § Harness log, § Application isolation, § Run the test suite | Yes | — |
| `applications/quizzes-full-2/quizzes-full-2-aggregate-grouping.md` | §2 snapshot fields, §4 events table and its two notes | No | §4 marks `quizAnswerAggregateId` as the anchor while its own payload note requires the consumer to learn that id *from* the event. Contradiction inside one table (row 71). |

---

## Skill Instructions Feedback

### What worked well

- The "two tests per subscribed event type" rule plus the deletion-event `and:`-block pattern produced 20 T3 cases with no ambiguity about shape.
- § "Deletion events: `remove()` on the whole consumer vs. remove a sub-entity" answered every one of the five deletion-shaped events here on its own: the distinguishing question ("can the consumer still fulfil its purpose?") splits creator/execution/quiz (structural, whole-consumer) from participant/topic (collection member) without further judgement.
- § "When to skip the guard" resolved the one no-guard case (`recordQuestionAnswerByEvent`) mechanically — the participant answer statistics are the one cached group no saga step writes.

### What was unclear or missing

- Nothing in the skill or docs anticipates the **anchor-unreachable** case: an aggregate-grouping spec that names a consumer for an event whose anchor id that consumer has no way to obtain. The skill's § Conditional subscriptions comes closest, but it describes an anchor that becomes non-null *later*; here it never does, so the null guard would silently produce a subscription that can never fire and a T3 test that cannot be written. This surfaced as a Type 2 halt.
- § "Always advance the cached publisher version" is written as if a cached publisher version served exactly one event type. When two event types share an anchor and one of them must not advance the shared snapshot version (advancing it hides the other's pending events), the doc offers no alternative cursor and no idempotence requirement to compensate.

### Suggested wording / structure changes

- `docs/concepts/events.md` § "Always advance the cached publisher version": add the two-event-types-one-anchor case — either the fold gets its own cursor field, or advancing is deferred to whichever event type the anchor version belongs to, and in the latter case the fold must compare the event version itself so redelivery is a no-op.
- `.claude/skills/implement-aggregate/session-d.md` § Shared-anchor events: state that the anchor must be an id the consumer already caches, and that a spec naming an anchor the consumer cannot reach is Type 2, not a conditional subscription.

---

## Semantic-Lock Coverage Audit (sessions `c` only — write "n/a" for `a`/`b`/`d`)

n/a

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `.claude/skills/implement-aggregate/session-d.md` | No handling for a declared consumer whose subscription anchor is unreachable; § Conditional subscriptions only covers anchors that become non-null later | High | State the reachability precondition on § Shared-anchor events and route the unreachable case to the Type 2 gate |
| `docs/concepts/events.md` | § "Always advance the cached publisher version" assumes one event type per cached publisher version; gives no rule when two event types share an anchor and advancing it would shadow the other | High | Add the separate-cursor / version-comparing-fold pattern, with the idempotence obligation it carries |
| `docs/concepts/events.md` | § ByEvent sagaState guard says nothing about an event whose fold trips a P1 invariant by construction (here `AnonymizeStudentEvent` on a creator vs `CREATOR_IS_NOT_ANONYMOUS`). § P2 rule enforcement in `session-d.md` says the exception propagates and the event stays unprocessed, i.e. it is retried forever | Medium | Say explicitly that a permanently-failing fold is the accepted outcome and belongs in a T3 invariant-violation case, or give the alternative |

---

## Patterns to Capture

- **Pattern:** Version-comparing idempotent fold
  **Observed in:** `microservices/tournament/service/TournamentService.java` (`recordQuestionAnswer`), `microservices/tournament/notification/subscribe/TournamentSubscribesQuizAnswerQuestionAnswer.java`
  **Description:** When a consumer holds several subscriptions on the same anchor whose version cursors advance independently, one event is redelivered under every trailing subscription. Guard the mutation with `cached.getVersion() != null && cached.getVersion() >= event.getPublisherAggregateVersion() → return`, so a counter-incrementing fold stays correct under redelivery. Use whenever the fold is not naturally idempotent (counters, appends) rather than a plain field overwrite.

- **Pattern:** Consistency-group fold
  **Observed in:** `microservices/tournament/service/TournamentService.java` (`setUserName`, `anonymizeUser`)
  **Description:** Where a P1 invariant requires two cached snapshots of the same upstream entity to agree (here `TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY` across the creator snapshot and any creator-shaped participant entry), one ByEvent mutation must touch every snapshot matching the payload's entity id, not only the first. Folding into one of them leaves the aggregate unable to commit.

- **Pattern:** Time-gated T3 fixture
  **Observed in:** `sagas/tournament/TournamentInterInvariantTest.groovy` (`startedTournamentWithAnswerableQuiz`)
  **Description:** Where the invariant guarding a folded field is temporal (`firstAnswerTime >= startTime`) and the state cannot be constructed directly, the fixture opens the shortest legal future window, completes every setup step that the pre-window invariants require, then waits the window out on the same clock the invariant reads. Sibling of the `createClosedTournament` helper 2.8.c introduced.

---

## Harness Changes

Rows appended to `applications/quizzes-full-2/harness-log.md` this session: 71, 72

| Row # | Type | Outcome | `harness:` commit |
|-------|------|---------|-------------------|
| 71 | 2 | deferred | - |
| 72 | 1 | fixed | 2b2b67ad3 |

---

## One-Line Summary

An aggregate-grouping spec can name a consumer for an event whose anchor that consumer can never obtain, and neither the skill nor `events.md` has a category for it — the harness treats every anchor as reachable and every cached publisher version as serving exactly one event type.
