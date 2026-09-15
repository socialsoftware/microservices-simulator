# Source-derived update/read inputs

User approved the preceding investigation's recommended package with “siga”. Use reviewed
execution on the current checkout: Sol/medium implementation; root integration and
verification, with independent review of the finished change. Preserve all inherited work.

## Outcome

Generate and execute FindTournament + UpdateTournament using preparation and concrete
inputs extracted from the existing `UpdateTournamentTest#update tournament successfully`.
Do not replace its inputs with a prerequisite provider or edit the Quizzes test to fit
the extractor. Preserve the requested dates and numberOfQuestions=3, then qualify the
compensated-update read witness and no-fault control through the ordinary executor.

## Generic change boundary

- Preserve supported ordered setters/property assignments applied before the target call
  to a DTO returned by an exact earlier source setup action, including through helpers.
- Resolve nested aggregateId references in supported DTO/collection input recipes using
  the existing exact producer occurrence and approved property authority. Missing, wrong,
  ambiguous or selected/later producers remain blocked. No guessing variable names/IDs.
- Preserve the prepared DTO value for other participant inputs: one input's changes must
  not silently mutate a shared setup result. Keep copy/alias semantics explicit and
  bounded to supported DTO values. Unsupported mutation/value forms must fail closed.
- Do not let later setters leak backward into earlier calls, discard setters on source
  result references, or treat a statically admitted input as proof of runtime correctness.
- Use explicit experiment selection of the existing successful pair to avoid the broad
  campaign's maxInputsPerSaga prefix; do not change global selection/scheduling defaults.

Necessary recipe/model/export/read/runtime details within this outcome are implementation
choices. No application/simulator behavior, score/anomaly semantics, GA objective, arbitrary
getter language, whole-test replay or general alias-analysis engine. The separate HashSet
constructor representation issue remains out unless unavoidably needed for this exact pair.

## Proof and documentation

Dummyapp-first extraction and executor tests for mutated returned DTOs, supported nested
ID properties, missing/wrong/late producers, before/after-call assignment isolation and
shared-result preservation. Verify actual materialized values, not just candidate counts.
Run the real existing Quizzes pair through source extraction, package write/read, source
setup and ordinary ScenarioExecutor, with successful control and assigned-fault positive.
Use the existing qualified Docker application build with explicit current verifier-source
provenance, fresh JVM/H2 and fixed clock. Update current-state/roadmap and record outcome,
limitations, actual files and evidence in HANDOFF.md. No commits/branches/PRs/pushes.

## Bounded implementation decisions

Reuse ACTION_RESULT with optional ordered assignments; persist these as `fields` on the
existing setup-result recipe. Apply approved setters to a shallow no-argument bean copy,
requiring public getters/setters for every copied property. This preserves other retained
bindings but does not implement arbitrary aliasing or nested in-place object mutation.
The existing DTO/property allowlist and duplicate-assignment rejection remain in force.
A property projection must reject a prior mutation of that same projected property;
unrelated setters must not prevent reading the original aggregateId. Malformed result
`fields` must fail closed at package projection. These are checks inside the approved
source-value preservation boundary, not a new application contract.
