# Bounded combined-event delivery handoff

Status: complete.

## Delivered

Generation now streams ordered subsets and legal placements of distinct consumer routes
of the exact same trigger/emission. `maxEventConsequencesPerWorkload` defaults to 1 for
compatibility and is exposed through Spring/YAML as
`verifiers.scenario-catalog.max-event-consequences-per-workload`. Values below 1 fail.
Configuration copies and accounting export retain the value. Existing base/singleton
identities are preserved; with combinations enabled the global cap can omit later
base/singleton workloads as well as combinations.

No simulator or executor production change was needed: the list-based action contract,
per-action replay scope and capture-by-trigger already permit successive routes. New
runtime tests establish that capability and its fail-closed limits. Existing prerequisite
descriptors still select singleton event workloads; the qualified combination path
uses one coherent source fixture, not automatic fixture composition.

## Changed surfaces

- ScenarioGenerator/config, ScenarioGeneratorApplication, YAML, config-copy paths in
  prerequisite generation/accounting/static export.
- Dummyapp event package tests and ScenarioExecutor tests.
- A scoped campaign under `verifiers/experiments/combined-event-deliveries/`, reusing the
  existing Docker build/run and report-validation machinery.
- Current-state, roadmap, event ADR, navigation and Portuguese meeting note; exact
  reports retained under `docs/verifiers-impl/evidence/combined-events-2026-09-06/`.

No production Quizzes fix, impact formula change, anomaly detector, GA, push or merge.
The user's original `note-04-09-2026.md` remains outside this change.

## Proof

- Full verifier: 798 tests across 48 suites, zero failures/errors/skips. See REGRESSION.json
  for exact suite counts/hashes and source report paths. Includes 161 executor tests,
  15 dummyapp event-package tests and 19 static boundary tests.
- Three generic routes produce 15 nonempty ordered selections after the final trigger;
  with one later outer step there are 6 singleton, 18 pair and 24 triple placements.
  A 30-route fixture under cap 33 stops after base + 30 singletons + two combinations.
- Eight fresh Docker attempts from one frozen source input/setup: base score2, each
  singleton score1, both combined orders score0; both trigger-fault variants COMPLETE1
  from residual Course count; a third missing receiver is INVALID/null after the first
  two handlers complete. Exact event identity and distinct action/receiver/writer
  evidence are retained. No scoring-policy changes.
- Full package checksums, all 1014 frozen source-copy hashes and all 19 durable copied
  evidence files verified. Current production sources match the Docker snapshot.
- Documentation builds and local links/anchors pass; diff whitespace check passes.
- Independent review PASS in REVIEW.md.

## Execution discoveries

The primary checkout's compiled classes/generated sources were being rewritten by the
active IDE, so local Maven validation initially encountered unrelated unresolved compiled
references and disappearing generated sources. Validation used an isolated source copy
with JDK21; no IDE settings/processes were changed, and no retained verifier target
reports were removed. Do not sum the stale primary target XML as this run's test result.
Logs: `verifiers/target/combined-event-deliveries/validation/`.

The first Docker build succeeded, but generator startup inherited Compose's
`SPRING_PROFILES` variable, which Spring rejects. The generator launcher now unsets it
and `SPRING_PROFILES_ACTIVE`, matching the established execution launcher. `run-01`
retains the pre-scenario failure. `run-02` is the complete eight-attempt campaign and
retains every execution, including the intentionally invalid missing-third case.

## Next

Review meeting-note section 3.4: it explains why completing both selected reactions
removes flags present at an earlier horizon. Section 4.6 and the roadmap place bounded
causal-anomaly flags as complementary future information, with exact read provenance
and positive/negative controls. They are not implemented or silently added to the score.
Proceed to cost/baseline preparation and bounded search without requiring arbitrary
fan-out, nested event chains or every anomaly type first.
