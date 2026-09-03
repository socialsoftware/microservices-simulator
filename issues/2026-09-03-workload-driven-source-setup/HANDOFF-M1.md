# M1 handoff — arity-independent setup applicability

State: `complete` after independent re-review.

## Outcome

Source setup attachment is no longer keyed to a left/right pair. The adapter now emits
one deterministic candidate per observed class-scoped `setup()` context, with the stable
set of InputVariant ids whose setup-dependent arguments it can completely bind. The
generator attaches that candidate only when it is the unique candidate covering the
complete selected tuple, then projects participant bindings to the exact workload while
reusing the full source-ordered action list.

This covers FR-1–FR-9: single, pair, and larger workloads use the same matcher; partial
pair containment and cross-test synthesis are rejected; runtime-owned and independently
ready arguments need no binding; unsupported values remain diagnostic; and every retained
source occurrence appears once, including supported void effects. Persisted setup,
workload, and accounting shapes are unchanged.

## Actual files changed

- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/model/SourceSetupPlanBinding.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/adapter/SetupPlanMapper.java`
- `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/scenario/ScenarioGenerator.java`
- `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/visitor/SourceDerivedSharedSagaWorkloadAnalysisSpec.groovy`
- this handoff

## Discovery and decisions

- Autonomous implementation detail: setup discovery now starts from observed `setup()`
  action contexts rather than `SourceSupportedSagaPairEvidence`; retaining the pair gate
  would have left one-participant contexts undiscoverable.
- Autonomous implementation detail: direct recipe source references are eligible setup
  bindings in addition to top-level producer references. This is required by the natural
  `UpdateStudentNameFaultTest` course-execution arguments and uses the existing closed
  SetupValueRecipe contract.
- Review pass 1 found that checking only mapper-level `executorReady` on unbound arguments
  could admit recipes the executor rejects. Candidate eligibility now delegates each
  unbound argument to `ScenarioExecutorReadinessEvaluator`, the existing runtime-owned and
  recipe-shape authority. A real StartQuiz property-access case pins exclusion and its
  source-facing incomplete-coverage diagnostic.
- Autonomous implementation detail: setup matching was added to the single-workload path;
  it had previously only been called during multi-Saga generation.
- One class-scoped Spock `setup()` is treated as one coherent observed setup context. Inputs
  from another source class cannot be covered by that candidate.
- Multiple candidates covering one tuple produce an ambiguity warning and no setup. No new
  resolver, package field, CLI option, accounting field, or application-specific dispatch
  behavior was added.
- No material scope delta or out-of-scope code finding was found. `lib/` and `tmp/` remain
  untracked and untouched. Canonical documentation remains M3 work.

## Proof

- Existing RemoveTournament/AddParticipant regression: ten workloads retain the exact
  12-action setup; the four void effects remain and selected bindings reference only the
  two workload inputs.
- M0 larger cross-test tuple: the same pair plus an unrelated CreateUser input now has no
  setup.
- Natural `UpdateStudentNameFaultTest`: one observed context covers AddStudent,
  GetCourseExecutionById, and UpdateStudentName; its single-participant and three-participant
  workloads receive setup, the size-three plan validates, and source occurrences are unique.
- `CreateTournamentStartQuizRecoveryWindowExploratoryTest` StartQuiz has an unbound argument
  marked ready by extraction whose placeholder-backed property recipe is rejected by the
  executor evaluator; it remains outside setup coverage with the expected blocker diagnostic.
- `mvn -q -Dtest=ScenarioGeneratorSpec,ScenarioModelSpec,ApplicationAnalysisScenarioModelAdapterSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec,DummyappAccountingFixtureFoundationSpec test`
  — pass, 160 tests.
- `mvn -q -DskipTests compile` — pass.
- `mvn -q test-compile` — pass.
- `git diff --check` — pass.
- The focused Quizzes spec writes and reads the current package through the checksum boundary;
  deterministic identity coverage in `ScenarioModelSpec` and byte-stable Dummyapp package
  coverage remain green.
- Independent review pass 1 found one readiness-authority blocker; the same implementer
  corrected it, and independent re-review pass 2 passed with no blocking findings.

## What to try

Review the three adjacent cases in `SourceDerivedSharedSagaWorkloadAnalysisSpec`: the
preserved pair, rejected cross-test triple, and natural UpdateStudentName single/triple.
Then run the M2 bounded size-1–3 Quizzes generation to measure how many emitted workloads
now reference these coherent setup candidates and which representatives pass preflight.

The main veto point is the class-scoped interpretation of one observed Spock `setup()`
context. It matches the current extractor facts and avoids cross-test synthesis; a narrower
feature-specific context would require new source evidence and is outside this milestone.
