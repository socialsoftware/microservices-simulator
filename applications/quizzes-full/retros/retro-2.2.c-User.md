# Retro — 2.2.c — User

**App:** quizzes-full
**Session:** 2.2.c (Read Functionalities)
**Date:** 2026-05-01

---

## Files Produced

- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/coordination/sagas/GetUserByIdFunctionalitySagas.java` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzesfull/microservices/user/coordination/functionalities/UserFunctionalities.java` (modified — appended `getUserById`)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzesfull/sagas/coordination/user/GetUserByIdTest.groovy` (created)
- `/home/kikoleitao/repos/pic/microservices-simulator/applications/quizzes-full/plan.md` (modified — ticked `2.2.c`)

---

## Docs Consulted

| Doc file | Sections used | Sufficient? | Notes |
|----------|--------------|-------------|-------|
| `docs/concepts/sagas.md` | § Read Functionality Sagas | Yes | Exact class template present; needed only minor variable substitution |
| `.claude/skills/implement-aggregate/session-c.md` | All sections | Partial | See skill feedback below |

---

## Reference App Consulted

| Reference file | Why consulted | Gap it reveals |
|---------------|--------------|----------------|
| `applications/quizzes/src/main/java/.../execution/coordination/sagas/GetCourseExecutionByIdFunctionalitySagas.java` | Confirmed read saga pattern: field stored via setter, plain `commandGateway.send` (no `SagaCommand` wrapper) | `docs/concepts/sagas.md` template uses direct field assignment (`this.{aggregate}Dto = ...`); reference uses a setter. Docs could note that either style is acceptable, or standardise on one. |

---

## Skill Instructions Feedback

### What worked well

- `session-c.md` clearly identifies that read sagas use no `SagaCommand` wrapper and need no compensation — this avoids the common mistake of copying the lock-acquisition pattern from write sagas.
- `docs/concepts/sagas.md` "Read Functionality Sagas" section provides a ready-to-fill template; the session was almost mechanical.

### What was unclear or missing

- `session-c.md` includes a step "Read methods appended to `{Aggregate}Service.java`" and "One `{Query}Command.java` per read functionality". For User, both were moved to session 2.2.b (plan.md note says so explicitly). `session-c.md` does not explain how to handle the case where service read methods and commands were pre-empted into session `b` — the implementer must infer from the plan.md file table whether these steps apply.
- `session-c.md` step "Read method appended to `{Aggregate}Functionalities.java`" is required but the corresponding file (`UserFunctionalities.java`) was not listed in the plan.md 2.2.c file table (it appeared in 2.2.b as the coordinator bean). The agent correctly identified the gap and updated it, but the plan.md was not patched to reflect this because the file already existed under 2.2.b. The skill gives no guidance on this edge case.

### Suggested wording / structure changes

- In `session-c.md`, add a note under "Read methods appended to `{Aggregate}Service.java`":
  > If plan.md notes that `getUserById` was moved to session `b` (e.g., because a write saga needed it for semantic lock acquisition), skip creating the service method and command. Still append the coordinator method to `{Aggregate}Functionalities.java` and create the `FunctionalitySagas` class and test.
- In `session-c.md`, under "Read method appended to `{Aggregate}Functionalities.java`": clarify that this modification must happen even if `{Aggregate}Functionalities.java` is not listed in the 2.N.c file table (because it was already created in session `b`).

---

## Documentation Gaps

| Doc | Missing / unclear | Impact | Suggested fix |
|-----|------------------|--------|---------------|
| `docs/concepts/sagas.md` | Template uses direct field assignment (`this.dto = ...`); reference app uses a setter (`this.setDto(...)`). Both work but the inconsistency makes comparison confusing. | Low | Standardise the template on direct assignment (simpler, no extra method needed) and note that a setter is acceptable if the field is also accessed within the saga. |

---

## Patterns to Capture

- **Pattern:** Read command pre-empted into session `b`
  **Observed in:** `applications/quizzes-full/plan.md` (aggregate 2, session 2.2.b notes)
  **Description:** When a write saga needs to fetch the primary aggregate before mutating it (the two-step lock-acquisition pattern), `Get{Aggregate}ByIdCommand` must exist before that write saga is coded. Plan.md moves the command to session `b`. Session `c` then only needs the `FunctionalitySagas` class and test; it must still add the coordinator method to `{Aggregate}Functionalities.java`. This edge case is not covered in `session-c.md`.

---

## Action Items

| Priority | Target file | Action |
|----------|------------|--------|
| High | `.claude/skills/implement-aggregate/session-c.md` | Add a note explaining that when plan.md moves `GetXxxByIdCommand` and service read method to session `b`, session `c` skips those two steps but must still produce `FunctionalitySagas`, the coordinator method in `XxxFunctionalities`, and the test. |
| Medium | `.claude/skills/implement-aggregate/session-c.md` | Clarify that `{Aggregate}Functionalities.java` must be updated in session `c` even if it is not listed in the plan.md 2.N.c file table (because it was created in session `b`). |
| Low | `docs/concepts/sagas.md` | Standardise the read saga template on direct field assignment and remove the inconsistency with the reference app's setter style. |

---

## One-Line Summary

Session `c` for User was minimal (command and service method had already been moved to session `b`), but `session-c.md` does not explain how to handle that pre-emption, so the agent had to infer the correct scope from plan.md.
