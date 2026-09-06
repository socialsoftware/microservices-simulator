# Nested participant setup bindings — implementation handoff

## Outcome

The existing participant binding projection now reconstructs an entire typed recipe when
that recipe contains exact earlier whole-result references. Constructors, ordered property
mutations, lists, sets, supported transforms and literal siblings are preserved. A nested
reference is admitted only when its occurrence and producer method match one retained
action; missing, later, selected-target, conflicting and wrong-type cases remain invalid.

The boundary is deliberately narrower than arbitrary nested property projection. Existing
root `ACTION_RESULT_PROPERTY` bindings remain supported, but a property path found only
inside a participant DTO/collection is blocked. This keeps CreateTournament's aggregate-id
helper collections outside this issue while admitting the intended returned QuestionDto and
TopicDto objects.

Setup/setupHelper targets with new nested dependencies no longer inherit the class's full
fixture plan. They receive an exact source prefix ending before their own facade occurrence.
Missing or nonmatching target occurrence metadata fails closed. Existing non-nested setup
candidate priority and the existing feature-frontier behavior remain unchanged.

## Exact Quizzes result

Final package:
`verifiers/target/astra-nested-bindings/final-generated/quizzes-20260905-125205-380/`.

- Accepted denominator: **796**.
- Source setup: **664** (baseline 576, gain 88).
- Without setup: **1** (unchanged).
- Blocked: **131** (baseline 219, reduction 88).
- Exact gained/lost input IDs: **88 gained / 0 lost**.
- Gained families: **85 CreateQuestion + 3 CreateQuiz**.
- The three CreateQuiz IDs are:
  - `010e1d7074088643dd0a8ec5b40b8075dc9b366ece056e4075ac09d464626326`
  - `a7191c8130c3fbd4d8c94ab1233865550629753d5ad8b46f2587dd522cab513c`
  - `df5ddb6614028d507eb7822bfeba23484e73dfa7f0c7248cf38474f9b94ca621`
- There are 87 accepted setup/setupHelper CreateQuestion target occurrences in the
  analysis model. Eighty-five are new candidates; two already had source setup. The raw
  package has 88 physical CreateQuestion rows when the rejected feature row is included.
- No CreateTournament, CreateTournamentAsync, UpdateTournament or other family gained.

The package still writes 944 workloads; event/provider additions are not part of the 796
ordinary-input denominator. It contains 2,521 initial fault scenarios with recovery cap 1.

Machine-readable evidence is under `verifiers/target/astra-nested-bindings/`:
`gained-input-ids.txt`, `lost-input-ids.txt`, `gained-inputs.json`,
`gained-by-saga.json`, the baseline/final setup ID sets, generation logs and
`determinism.json`. The earlier `mapper-only/` and first `generated/` packages are audit
artifacts that exposed unsafe full-fixture replay and the deferred scalar-property families;
they are not qualification packages.

## Incremental changed files

Relative to `verifiers/target/astra-nested-bindings/starting-tracked.patch`, this work changed:

- `verifiers/src/main/java/.../scenario/adapter/SetupPlanMapper.java`
- `verifiers/src/main/java/.../scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java`
- `verifiers/src/main/java/.../visitor/GroovyConstructorInputTraceVisitor.java`
- `verifiers/src/main/java/.../scenario/SetupPlanValidator.java`
- `applications/dummyapp/src/main/java/.../order/coordination/CreateOrderFunctionalitySagas.java`
- `applications/dummyapp/src/test/groovy/com/example/dummyapp/GroovySagaTracingSpec.groovy`
- `verifiers/src/test/groovy/.../scenario/adapter/SetupPlanMapperNestedParticipantSpec.groovy` (new)
- `verifiers/src/test/groovy/.../scenario/adapter/SetupPlanMapperNestedPropertySpec.groovy`
- `verifiers/src/test/groovy/.../visitor/SourceDerivedSharedSagaWorkloadAnalysisSpec.groovy`
- `verifiers/src/test/groovy/.../visitor/WorkflowFunctionalityVisitorSpec.groovy`

The dummyapp validator entry authorizes only the canonical fixture's `ItemDto` fields so
the parsed source proof can pass the same closed validator as Quizzes. It does not add a
generic DTO grammar. The fixture adds a distinct six-argument constructor solely to expose
a `List<ItemDto>` participant without perturbing existing overloaded-constructor inference.

## Proof

- Focused mapper, parsed dummyapp visitor/frontier and target replay suite:
  **57 tests, zero failures/errors/skips** (`focused-tests.log`).
- Quizzes source-derived integration test: **1 test, pass**
  (`quizzes-focused-test.log`). It asserts validated exact pre-target plans for all 87
  CreateQuestion setup targets and all three CreateQuiz targets.
- Post-fixture inventory focus: pass (`post-full-fix-focused.log`).
- Full verifier suite: **734 tests, zero failures/errors/skips**
  (`full-verifier-tests.log`).
- This fresh Maven summary supersedes the earlier event-receiver handoff's **778** count.
  Both runs used the same default Surefire selection (`mvn test`, no `-Dtest`, no active
  profile), but the earlier quiet run's count was derived from a non-clean
  `target/surefire-reports/` directory. Five XML files dated 2026-08-28 represented
  suites that no longer exist in `src/test` and contributed 53 stale results. The current
  run executed every one of the 47 present `*Spec`/`*Test` classes; its authoritative
  Maven total is 734. Before this issue's nine new tests, the equivalent current-source
  total was therefore 725, and 725 + 53 explains the reported 778 exactly.
- Repeated ordinary generation produced byte-identical copies of all nine package files:
  `final-generated/quizzes-20260905-125205-380/` and
  `repeat-generated/quizzes-20260905-125859-988/`.
- `git diff --check`: pass.

Generation used application `quizzes`, size 1, max inputs 1000, max catalog 5000,
recovery cap 1 and dynamic enrichment disabled.

## Review and remaining proof

The generic negative suite covers missing, later/selected-target, conflicting occurrence,
wrong type, root-reference historical provenance and no-reference regression. Existing
InputTupleSelection coverage rejects target replay. There is not a separate synthetic
adapter unit that removes target occurrence metadata; the adapter has an explicit fail-closed
branch, and the parsed Quizzes integration asserts one exact target occurrence for every
new target-bound plan. Nested scalar-property exclusion is proven by the final zero-gain
Tournament delta, but does not have a dedicated one-line mapper test.

Docker preflight/replay and exact runtime identity checks are intentionally left to the root
review task. This handoff makes no runtime-success or ImpactV1 claim. No commits, pushes,
merges, deployment or production action were performed.
