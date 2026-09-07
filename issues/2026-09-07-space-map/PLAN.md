# Execution and proof

1. Generate all sizes 1–3 under strict interaction pruning, 10 accepted inputs per Saga, one SERIAL schedule per tuple, up to three event consequences, and a 50,000 workload cap. Report cap accounting. This is a bounded input catalogue, not all Quizzes inputs. Select across distinct Saga combinations rather than a global prefix.
2. Eight structural strata: AddStudent/RemoveStudentFromCourseExecution; AddStudent/UpdateStudentName; UpdateStudentName/AddParticipant; RemoveStudentFromCourseExecution/UpdateStudentName; GetCourseExecutionById/UpdateStudentName/AddParticipant; AnonymizeStudent/GetCourseExecutionById/RemoveStudentFromCourseExecution; AddParticipant/SolveQuizAsync; AddStudent/AddParticipant/SolveQuizAsync. Require a source-derived materializable setup and eager executable candidates. Prefer maximum event delivery count, then choose by seeded sampling over sorted workload IDs (seed 9072026). Preserve every candidate and selection reason in the inventory. Missing strata remain explicit failures, with no replacement.
3. Enumerate canonical vectors with at most one failed forward step per participant. Up to 24 vectors per workload: always retain all-zero and all single-point controls; seeded sample the remaining vectors. Request up to 10,000 recovery schedules for each selected vector through the existing on-demand service. Report uncapped versus written counts; any cap is a supported-generator prefix limitation, never a uniform full-space sample.
4. Enumerate all persisted candidates when at most 32. Otherwise choose one seeded scenario per requested vector, then seeded sample additional candidates to 32. Freeze IDs and package/build hashes before execution. No score feedback influences discovery selection. Maximum 256 discovery attempts.
5. After discovery, repeat once up to three representatives per workload: lowest scenario ID in each distinct status/score/three-condition signature, sorted by that signature. Maximum 24 extra attempts, reported separately. This measures representative repeatability only.
6. Validate sampling invariants and report joins, unique attempt IDs, score eligibility, snapshot integrity and hashes. Preserve raw logs/reports under target; commit compact evidence, scripts, technical results, handoff, and affected canonical documentation locally. No push, merge, worktree, meeting-note edit or Java production change.

The prepared build differs from the current checkout only by the later GroovyClosureBoundarySpec test. Production sources are identical; record this explicitly and verify the complete immutable build before and after.

## Corrective follow-up plan

1. Join each original invalid report to its setup, participants, ordered event routes,
   and eligible runtime objects. Separate repeated-target setup failures, other unassigned
   application failures, and selected-route receiver absence.
2. Extend setup-target recognition from the previous nested-reference subset to every
   exact adapted facade occurrence observed in `setup()`. Reject missing or ambiguous
   occurrence evidence.
3. For a selected setup target, map only actions before its occurrence. Bind the target
   and any compatible same-class participants to that prefix, and retain frontier metadata
   so tuples that do not select the target continue to use their ordinary complete setup.
   Leave tuples without one coherent prefix blocked.
4. Add dummyapp-first ownership/no-replay coverage and real Quizzes positive/negative
   coverage, including the AddStudent/RemoveStudent tuple and preservation of unrelated
   complete setups. Run focused Spock tests in a private snapshot and Maven cache.
5. Build and generate from a frozen Docker-visible copy. Execute only the corrected `w01`
   no-fault control, a source-backed Tournament receiver no-fault control, and that
   control's single trigger-fault variant. Do not rerun the 159-case campaign.
6. Record structural equivalents, report/package hashes, the six-group classification,
   remaining receiver gaps, and the deliberately blocked `w08` tuple. Update canonical
   current state, Outcome 6, results, findings, and handoff; review the final diff and
   commit only the scoped files locally.
