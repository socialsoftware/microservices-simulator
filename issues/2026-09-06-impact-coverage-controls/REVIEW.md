# Independent review

## Observer slice

**Status: PASS.**

Compared with `/tmp/impact-coverage-controls-base`, the observer delta is bounded to
owned inverse JPA back-links. A repeated entity is treated as an owned ancestor only
when the parent declares an inverse `@OneToOne`/`@OneToMany` mapping with persistence
ownership and actually contains the child. The projection keeps a stable relative
ancestor reference without generated row IDs or unordered collection indexes, so
changing a meaningful child field still changes normalized application data. Unsupported
cycles retain `PERSISTENT_MAPPING_CYCLE` and `cycleRef`.

The Quizzes `TournamentParticipant -> ParticipantAnswer -> TournamentParticipant`
shape matches that contract. The focused artifacts report 8/8 passing (7 observer tests
and the database precision regression), and the root reports the simulator suite at
136/136. No scoring, lifecycle, timestamp, dependency, event, or application behavior
change was found in this slice.

## Prerequisite and qualification slices

### Prerequisite source slice

The bounded reader/materializer and Quizzes fixture changes pass their focused proof:

- `CurrentExecutableArtifactContractSpec` plus `ScenarioDateMaterializationSpec`: 35/35;
- the refreshed Quizzes source fixture/dispatcher tests: 7/7;
- the generated source package was independently checked for 956 workloads and the
  selector can freeze 60 broader rows plus 29 benchmark rows from it without using
  outcomes.

The test-only `PersistentStateObserver(EntityManager)` beans in
`BeanConfigurationSagas` and `BeanConfigurationCausal` are necessary context wiring for
the inherited observer dependency and do not alter Quizzes production configuration.
The closed dispatcher authorizes only the reviewed setup methods, including the exact
two-Integer disenrollment prefix needed to make the course-execution deletion fixture
valid. No Quizzes production source, scoring, event horizon, or extra event drain changed.

The source gate is now **PASS**. The initially omitted RemoveCourse prefix was moved to a
dedicated source fixture whose `setup()` contains the complete healthy path. The final
package audit records ten actions in source order: the ordinary QuizAnswer fixture, the
second `ACRONYM_1` offering, and the exact original-course/user disenrollment. The measured
`removeCourseExecution` action is absent from setup. The final focused fixture proof is
3/3, and the selected workload is marked materializable.

### Qualification harness static findings

The second pass fixes the two harness integrity findings. Both frozen selections carry the
final package manifest hash and `run.py` checks both before execution; the runner also rejects
duplicate `executionAttemptId` values after attempts complete. Static checks pass for 89 unique
cases, 30 complete broader pairs, package/selection joins, canonical vectors, and the ten
action RemoveCourse setup audit. This was the static-pass boundary; final runtime evidence
is recorded below.

### Benchmark and documentation evidence pass

The finished benchmark was independently checked against the frozen selection and package
manifest. All 29 execution/ImpactV1/ImpactV2 report triplets pass the shared identity,
nullable-count, category-coverage and distinct-object validation; process exit is zero and
attempt IDs are unique. The results are 29 `COMPLETE` assessments with 14 score-0 and 15
score-2 reports. Conformance is `EXACT` for 28 reports and the recorded `DEVIATED` path is
the expected zero-bit domain-failure route with an explicit blocker, not a missing report.
`benchmark-comparison.json` independently agrees that prior complete scores and execution,
conformance and implicit-rollback lists are unchanged.

The fresh CreateCourseExecution reports support the current domain note: the successful
control is `SUCCESS/EXACT/COMPLETE` with score 0, while the late-fault run is
`COMPENSATED/EXACT/COMPLETE` with score 1 on the deleted `SagaExecution(3)` residual. The
note presents that as a deferred policy question rather than business-harm proof. The
current-state ImpactV2 mechanism description, HANDOFF and DOMAIN-CASES links agree with
these artifacts; the later broader runtime gate is recorded below.

The initial broader run exposed one concrete prerequisite: the selected
`UpdateStudentName-events-control` setup succeeds, but its selected delivery has no eligible
subscriber (`SELECTED_SUBSCRIBER_NOT_FOUND`), so the attempt is
`UNEXPECTED_EXECUTION_FAILURE/INCOMPLETE/INVALID` with null score. Its paired late-fault
attempt is `COMPLETE/0`. The retained invalid attempts and the corrected route-corrected pair
are accounted for in the final integrated gate below. This is an execution-prerequisite issue,
not evidence for a zero-impact control.

### Final integrated runtime evidence

**Evidence gate: PASS.** Independent validation of the final qualification aggregate confirms
60 selected broader reports are present and `COMPLETE`, grouped into 30 complete pairs with
30 successful exact controls. The selected score counts are 52 zero, 7 one and 1 two. The
benchmark remains 29 `COMPLETE` reports with the previously recorded 14/15 score split.

The two unsuccessful prerequisite runs were retained rather than replaced: the original
run-01 `UpdateStudentName-events-control` and the supplementary run-02 control both preserve
their reports, exit code, `SELECTED_SUBSCRIBER_NOT_FOUND` blocker, `INCOMPLETE` conformance and
`INVALID` ImpactV2 status with null score. Across benchmark, broader and supplementary
artifacts there are 93 unique attempts: 91 `COMPLETE` and 2 `INVALID`.

The route-corrected run-03 pair is independently coherent. Its setup audit selects the
`AnonymizeStudentAndSolveQuizTest` source fixture, creates the Tournament and participant,
and records the exact `UpdateStudentNameEvent` route to `TournamentEventHandling` and
`UpdateStudentNameEventHandler`. Both reports are complete with score 0 and exact identity
joins; the control records `SagaTournament(12)` as the receiver, eligible before delivery
and ineligible at the final horizon after the receiver state changed. This corrected pair
supersedes interpretation of the earlier invalid control while preserving that invalid
attempt in the retained evidence.

The unaffected-selection comparison also passes: all 58 unaffected broader executable rows
and all 29 benchmark rows retain the same executable recipes/actions; only opaque IDs in the
two SolveQuiz rows changed.

No scoring-policy or production Quizzes claim follows from the aggregate. Documentation
synchronization is satisfied: HANDOFF/current-state/roadmap publish the final 60-report,
30-pair selection provenance, the eight replacements, and retained 93-attempt accounting.
