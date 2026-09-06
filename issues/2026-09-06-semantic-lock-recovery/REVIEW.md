# Semantic-lock recovery review

**Source review: PASS**, including the root-requested bounded subtype follow-up
after the initial pass-3 review. No remaining in-scope code blocker was found.
Root completion record: the final full verifier suite passed 773 tests across 48 suites,
with zero failures/errors/skips. Canonical current-state and roadmap updates are complete;
the final integration evidence is in `HANDOFF.md`.

Reviewed the approved `BRIEF.md`, relevant current-state terms and behavior, runtime
Saga command/recovery contracts, and this change against
`/tmp/semantic-lock-recovery-base`. Inherited ImpactV2 changes were not re-reviewed.
The reviewer ran no Maven or Docker commands and changed only this review record.

- The visitor retains payload access and adds a same-target `WRITE` for supported
  dispatched semantic-state configuration, without interpreting enum names.
  Forbidden-state-only and post-send configuration controls remain reads.
- Review initially found false confidence from conditional null overrides, aliases,
  reassignment, and loop reuse. The final bounded configuration checks make those
  unsupported cases incomplete instead of confidently effect-free. Regression
  coverage includes conditional override, both alias forms, and loop reuse.
- The bounded follow-up found that subtype constructors could configure status
  outside the lambda while their sends were treated as transparent. The final
  guard accepts only exact framework `SagaCommand` construction without an
  anonymous class body for precise configuration analysis. Subtype and anonymous
  initializer fixtures now produce explicit incomplete configuration analysis;
  no constructor tracing was introduced.
- Existing footprints feed conflict analysis and implicit recovery checkpoints;
  the dummyapp adapter/conflict/recovery assertions and Quizzes source assertions
  cover the resulting behavior without changing pipeline or package contracts.
- The executor queries runtime recovery before every new transition to
  `COMPENSATED`, including no planned recovery, the last scheduled checkpoint,
  and supported runtime fallback. Pending work or discovery failure hard-stops
  with an explicit reason and a null ImpactV2 score. It neither runs unplanned
  assigned-fault recovery nor changes normal commit behavior.

Read the final Surefire XML: `ScenarioExecutorSpec` has 148 passing tests and
`WorkflowFunctionalityVisitorSpec` has 37, with zero failures, errors, or skips.
The static handoff records the combined adapter/conflict/recovery/Quizzes source
run; the final visitor rerun covers the review corrections. A whitespace check
of both changed production files passed.

Read the saved runtime `summary.json`: all 29 corrected schedules exited normally,
23 `getTournamentStep` implicit rollbacks were observed, and the old `01001`
missing-checkpoint control returned `INCOMPLETE`, `PENDING_RUNTIME_RECOVERY`,
`INVALID`, and a null score. This does not mean every corrected ImpactV2 assessment
was complete: the summary retains 19 `COMPLETE` and 10 `PARTIAL` assessments.

The runtime campaign used the frozen snapshot before the final visitor-only subtype
guard. Read `final-package-comparison.json`: all nine recorded regenerated package
hashes are identical, and runtime executor, simulator, and application production
sources are unchanged. That supports carrying forward the 29 runtime observations
for the identical generated experiment; it does not claim the final visitor was
compiled into that earlier runtime build. Final source/test and provenance records
remain in the root handoff.
