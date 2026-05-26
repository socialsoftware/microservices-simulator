# Test Review — Tournament

**Date:** 2026-05-26
**Verdict:** Yellow — 3 tests added, no fakes/wrong findings; one design observation about unconsumed Tournament semantic locks; TOURNAMENT_AFTER_END guard deferred (test infrastructure cannot move clock).

---

## 1. Summary

- 3 missing tests added covering READ_QUIZ forbidden state, SolveQuiz cancellation guard, and delete with participants
- 0 Fake / Wrong / Weak tests detected
- All 44 tests pass: BUILD SUCCESS
- Two design observations:
  - All four Tournament `setSemanticLock` states (`IN_ADD_PARTICIPANT`, `IN_UPDATE_TOURNAMENT`, `IN_CANCEL_TOURNAMENT`, `IN_DELETE_TOURNAMENT`) have no `setForbiddenStates` consumer anywhere in the codebase — locks are acquired but never enforced via a foreign-aggregate conflict check. The existing lock-acquisition tests are the maximum feasible coverage; an interleaving test in the strict sense is not possible.
  - `TournamentService.setParticipantQuizAnswer` guard `TOURNAMENT_AFTER_END` is not covered: creating a tournament with an already-past `endTime` is rejected by `CreateQuizCommand` (it forces `availableDate >= now+1s` which then exceeds `endTime`), and post-creation mutation of `endTime` is blocked by `TOURNAMENT_FINAL_AFTER_START`. Covering this path requires test-time clock manipulation that is not available in this harness. Deferred.

---

## 2. Expected-Test Inventory

| Tier | Functionality / Rule | Required Scenario | Test Exists? | Test Name (if exists) | Defect caught if absent |
|------|----------------------|-------------------|--------------|-----------------------|-------------------------|
| T1 | Tournament | Create with valid data, assert all fields | Yes | "create tournament" | constructor doesn't set fields |
| T1 | TOURNAMENT_ANSWER_BEFORE_START (extra) | violating participant.firstAnswerTime triggers invariant | Yes | "answer before start violates invariant" | invariant skipped on linked answers |
| T1 | TOURNAMENT_ANSWER_BEFORE_START (extra) | answer after start passes | Yes | "answer after start satisfies invariant" | invariant false-positive |
| T2 | createTournament | Happy-path success | Yes | "createTournament: success" | tournament not persisted |
| T2 | createTournament / P1 TOURNAMENT_START_BEFORE_END_TIME | startTime > endTime rejected | Yes | "createTournament: TOURNAMENT_START_BEFORE_END_TIME violation" | invalid date order accepted |
| T2 | createTournament / P3 TOPIC_COURSE_EXECUTION | topic from other course rejected | Yes | "createTournament: TOPIC_COURSE_EXECUTION violation — topic from different course" | unrelated topic accepted |
| T2 | createTournament / P4a CREATOR_COURSE_EXECUTION | non-enrolled creator rejected (functional path) | Yes | "createTournament: CREATOR_COURSE_EXECUTION violation — creator not enrolled" | unenrolled creator accepted |
| T2 | createTournament / P4a CREATOR_COURSE_EXECUTION | non-enrolled creator rejected (step path) | Yes | "createTournament: getStudentStep enforces CREATOR_COURSE_EXECUTION check" | step bypassed |
| T2 | addParticipant | Happy-path success | Yes | "addParticipant: success" | participant not added |
| T2 | addParticipant / P1 TOURNAMENT_UNIQUE_AS_PARTICIPANT | duplicate participant rejected | Yes | "addParticipant: TOURNAMENT_UNIQUE_AS_PARTICIPANT violation — participant already enrolled" | duplicate enrollment silently accepted |
| T2 | addParticipant / P1 TOURNAMENT_ENROLL_UNTIL_START_TIME | enroll after start rejected | Yes | "addParticipant: TOURNAMENT_ENROLL_UNTIL_START_TIME violation — tournament already started" | late enrollment accepted |
| T2 | addParticipant / P1 TOURNAMENT_IS_CANCELED | add to cancelled rejected | Yes | "addParticipant: TOURNAMENT_IS_CANCELED violation — tournament already cancelled" | cancelled tournaments accept enrollments |
| T2 | addParticipant / P4a PARTICIPANT_COURSE_EXECUTION | non-enrolled participant rejected | Yes | "addParticipant: PARTICIPANT_COURSE_EXECUTION violation — participant not enrolled" | unenrolled participant accepted |
| T2 | addParticipant / setSemanticLock(IN_ADD_PARTICIPANT) | lock acquired (no consuming step exists) | Partial | "addParticipant: getTournamentStep acquires IN_ADD_PARTICIPANT semantic lock" | design gap: lock never conflict-checked |
| T2 | updateTournament | Happy-path success (mutates times) | Yes | "updateTournament: success — update times" | new dates not persisted |
| T2 | updateTournament / P1 TOURNAMENT_START_BEFORE_END_TIME | inverted dates rejected | Yes | "updateTournament: TOURNAMENT_START_BEFORE_END_TIME violation" | invalid dates accepted |
| T2 | updateTournament / P1 TOURNAMENT_FINAL_AFTER_START | mutate after start rejected | Yes | "updateTournament: TOURNAMENT_FINAL_AFTER_START violation — tournament already started" | post-start edits silently accepted |
| T2 | updateTournament / P1 TOURNAMENT_IS_CANCELED | mutate cancelled rejected | Yes | "updateTournament: TOURNAMENT_IS_CANCELED violation — tournament already cancelled" | cancelled tournaments mutable |
| T2 | updateTournament / setSemanticLock(IN_UPDATE_TOURNAMENT) | lock acquired (no consuming step exists) | Partial | "updateTournament: getTournamentStep acquires IN_UPDATE_TOURNAMENT semantic lock" | design gap |
| T2 | updateTournament / setForbiddenStates([IN_UPDATE_QUIZ,READ_QUIZ]) | IN_UPDATE_QUIZ forbidden state | Yes | "updateTournament: updateQuizStep sees forbidden state when quiz is locked by concurrent updateQuiz" | concurrent quiz update not detected |
| T2 | updateTournament / setForbiddenStates([IN_UPDATE_QUIZ,READ_QUIZ]) | READ_QUIZ forbidden state | **No → Added** | "updateTournament: updateQuizStep sees forbidden state when quiz is locked by concurrent createQuizAnswer" | concurrent read-lock not detected |
| T2 | solveQuiz | Happy-path success — links quiz answer | Yes | "solveQuiz: success — links participant quiz answer to tournament" | answer link not persisted |
| T2 | solveQuiz / QUIZ_ANSWER_NOT_FOUND | participant has no answer | Yes | "solveQuiz: QUIZ_ANSWER_NOT_FOUND — no quiz answer for participant" | missing answer silently accepted |
| T2 | solveQuiz / TOURNAMENT_IS_CANCELED guard | cancelled tournament rejected | **No → Added** | "solveQuiz: TOURNAMENT_IS_CANCELED — solving on a cancelled tournament is rejected" | cancelled tournaments still accept answers |
| T2 | solveQuiz / TOURNAMENT_AFTER_END guard | submission after end rejected | Deferred | (see Summary) | late submissions accepted |
| T2 | cancelTournament | Happy-path success | Yes | "cancelTournament: success" | cancellation not persisted |
| T2 | cancelTournament / P3 TOURNAMENT_IS_CANCELED guard | second cancel rejected | Yes | "cancelTournament: TOURNAMENT_IS_CANCELED violation — already cancelled" | double-cancel allowed |
| T2 | cancelTournament / setSemanticLock(IN_CANCEL_TOURNAMENT) | lock acquired (no consuming step exists) | Partial | "cancelTournament: getTournamentStep acquires IN_CANCEL_TOURNAMENT semantic lock" | design gap |
| T2 | deleteTournament | Happy-path success — no participants | Yes | "deleteTournament: success — no participants" | aggregate not removed |
| T2 | deleteTournament | Happy-path — cancelled with no participants | Yes | "deleteTournament: success — cancelled tournament with no participants" | cancellation+delete combo blocked |
| T2 | deleteTournament | Happy-path — clears participants on delete | **No → Added** | "deleteTournament: success — clears participants on delete" | non-empty delete leaks state |
| T2 | deleteTournament / P1 TOURNAMENT_IS_CANCELED | delete cancelled with participants rejected | Yes | "deleteTournament: TOURNAMENT_IS_CANCELED violation — cancelled tournament with participants" | cancelled-with-participants gets cleared |
| T2 | deleteTournament / setSemanticLock(IN_DELETE_TOURNAMENT) | lock acquired (no consuming step exists) | Partial | "deleteTournament: getTournamentStep acquires IN_DELETE_TOURNAMENT semantic lock" | design gap |
| T2 | getTournamentById | Happy-path success | Yes | "getTournamentById: success" | DTO incorrect |
| T2 | getTournamentById | Not-found (Path A: SimulatorException) | Yes | "getTournamentById: aggregate not found" | infra failure swallowed |
| T2 | getOpenTournaments | Happy-path — returns open tournament | Yes | "getOpenTournaments: success — returns open tournament for execution" | open tournaments not returned |
| T2 | getOpenTournaments | Cancelled excluded | Yes | "getOpenTournaments: cancelled tournament is not returned" | cancelled tournaments leak into open list |
| T2 | getOpenTournaments | Different execution returns empty | Yes | "getOpenTournaments: returns empty list for different execution" | wrong-execution tournaments leak |
| T3 | DeleteUserEvent (creator) | tournament deleted | Yes | "tournament is deleted on DeleteUserEvent for creator" | creator-deletion not propagated |
| T3 | DeleteUserEvent | unrelated user ignored | Yes | "tournament ignores DeleteUserEvent for unrelated user" | wrong-user delete propagated |
| T3 | UpdateStudentNameEvent (creator) | name reflected | Yes | "tournament reflects UpdateStudentNameEvent for creator" | stale creator name |
| T3 | UpdateStudentNameEvent | unrelated user ignored | Yes | "tournament ignores UpdateStudentNameEvent for unrelated user" | wrong-user update propagated |
| T3 | AnonymizeStudentEvent (creator) | creator anonymized | Yes | "tournament reflects AnonymizeStudentEvent for creator" | anonymization missed |
| T3 | AnonymizeStudentEvent | unrelated user ignored | Yes | "tournament ignores AnonymizeStudentEvent for unrelated user" | wrong-user anonymized |
| T3 | UpdateTopicEvent | topic name reflected | Yes | "tournament reflects UpdateTopicEvent" | stale topic name |
| T3 | UpdateTopicEvent | unrelated topic ignored | Yes | "tournament ignores UpdateTopicEvent for unrelated topic" | wrong-topic update propagated |
| T3 | DeleteTopicEvent | topic removed | Yes | "tournament removes topic on DeleteTopicEvent" | deleted topic still attached |
| T3 | DeleteTopicEvent | unrelated topic ignored | Yes | "tournament ignores DeleteTopicEvent for unrelated topic" | wrong-topic removed |
| T3 | DeleteCourseExecutionEvent | tournament deleted | Yes | "tournament is deleted on DeleteCourseExecutionEvent" | execution delete not propagated |
| T3 | DeleteCourseExecutionEvent | unrelated execution ignored | Yes | "tournament ignores DeleteCourseExecutionEvent for unrelated execution" | wrong-execution delete propagated |
| T3 | InvalidateQuizEvent | tournament deleted | Yes | "tournament is deleted on InvalidateQuizEvent" | invalid quiz still owned |
| T3 | InvalidateQuizEvent | unrelated quiz ignored | Yes | "tournament ignores InvalidateQuizEvent for unrelated quiz" | wrong-quiz invalidation |
| T3 | QuizAnswerQuestionAnswerEvent | participant answered count reflected | Yes | "tournament reflects QuizAnswerQuestionAnswerEvent for linked participant" | answer count never refreshed |
| T3 | QuizAnswerQuestionAnswerEvent | unlinked quiz answer ignored | Yes | "tournament ignores QuizAnswerQuestionAnswerEvent for unlinked quiz answer" | wrong-participant answered |

---

## 3. Quality Findings

| Test file | Test name | Issue type | Finding | Severity |
|-----------|-----------|------------|---------|----------|

No Fake, Wrong, or Weak findings. All existing tests:
- Use error-message constant assertions on violations (`ex.message == CONSTANT`)
- Are wired through public functionalities (no service-bypass shortcuts)
- Have correct `@DataJpaTest @Transactional @Import(LocalBeanConfiguration)` annotations
- Use the `and:` extension-block pattern on T3 deletion tests so the load attempt sits in exception-capture scope
- Use Path A `thrown(SimulatorException)` correctly on `getTournamentById` not-found (service calls `aggregateLoadAndRegisterRead` directly)

---

## 4. Edge Cases Added

- `updateTournament: updateQuizStep sees forbidden state when quiz is locked by concurrent createQuizAnswer` — covers the `READ_QUIZ` member of the `setForbiddenStates([IN_UPDATE_QUIZ, READ_QUIZ])` list which was previously untested.
- `solveQuiz: TOURNAMENT_IS_CANCELED — solving on a cancelled tournament is rejected` — covers the `setParticipantQuizAnswer` service guard that prevents linking answers on a cancelled tournament.
- `deleteTournament: success — clears participants on delete` — verifies the delete path on a non-cancelled tournament with participants actually clears participants and removes the aggregate (previously only the empty and cancelled cases were tested).

---

## 5. Tests Added

**UpdateTournamentTest.groovy:**
- `updateTournament: updateQuizStep sees forbidden state when quiz is locked by concurrent createQuizAnswer`

**SolveQuizTest.groovy:**
- `solveQuiz: TOURNAMENT_IS_CANCELED — solving on a cancelled tournament is rejected`

**DeleteTournamentTest.groovy:**
- `deleteTournament: success — clears participants on delete`

---

## 6. Tests Fixed

None.

---

## 7. Build Result

```
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

TournamentTest:                   3/3
TournamentInterInvariantTest:     16/16
CreateTournamentTest:             5/5
AddParticipantTest:               6/6
SolveQuizTest:                    3/3
UpdateTournamentTest:             6/6
CancelTournamentTest:             3/3
DeleteTournamentTest:             5/5  (includes 1 new)
GetTournamentByIdTest:            2/2
GetOpenTournamentsTest:           3/3
```
