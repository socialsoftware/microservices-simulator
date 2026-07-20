# Dynamic unmatched fixture diagnostics

## Slug

`dynamic-unmatched-fixture-diagnostics`

## Baseline

Comparable Quizzes run:

```text
verifiers/target/2026-06-29-dynamic-baseline-test-profile/quizzes-20260629-222801-046/
scenario records: 584
dynamicEventsRead: 26820
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
AMBIGUOUS: 0
UNMATCHED: 184
```

Audit summary:

```text
helper/setup create* unmatched: 165
other unmatched:                 19
```

Root issue:

```text
static provenance:  TestClass#createUser
runtime identity:   TestClass#"feature name"
runtime saga/step:  CreateUserFunctionalitySagas#createUserStep
```

Spock reports the active feature while `setup()` and helpers run. Runtime does not naturally report `setup` or helper method names.

## Final decisions

- Keep existing `DynamicEvidenceJoinStatus` enum unchanged.
- Add `dynamicEvidence.unmatchedReason` only for `joinStatus=UNMATCHED`.
- Reason values:
  - `FAILED_TEST_CLASS`
  - `NOT_SELECTED_TEST_CLASS`
  - `HELPER_OWNER_MISMATCH`
  - `UNCLASSIFIED`
- Add small additive input metadata:
  - `inputRole`: `FEATURE_UNDER_TEST`, `FIXTURE_PREREQUISITE`, `UNKNOWN`
  - `fixtureOrigin`: `DIRECT_FEATURE`, `SETUP`, `SETUP_HELPER`, `FIELD`, `SETUP_SPEC`, `INHERITED_SETUP`, `INHERITED_HELPER`, `INHERITED_FIELD`, `UNKNOWN`
- Do not redesign `ScenarioPlan` into prerequisite graphs.
- Preserve helper/source provenance; add call-context and role metadata.
- Use dummyapp-first TDD; Quizzes is final effectiveness validation.

## Goals

1. Explain current `UNMATCHED` records with deterministic reason counts.
2. Distinguish provenance, owner, call context, and fixture role in artifacts.
3. Mark setup/helper-created inputs as fixture prerequisites when statically justified.
4. Let setup/helper fixture inputs match the active Spock feature owner.
5. Keep honest ambiguity for same-feature sibling fixture calls.
6. Use Quizzes after dummyapp to measure effectiveness and capture remaining unmatched/ambiguous examples for later interpretation.

## Non-goals

- No runtime payload/aggregate-value matching in this issue.
- No fuzzy matching to force exactness.
- No scenario executor, fault injection, scoring, GA, or bandit work.
- No semantic deduplication of value-equivalent inputs.
- No schedule/accounting/segment-compression refactor.
- No stream/gRPC/distributed/TCC dynamic parity.
- No Quizzes production-code changes.

## Invariants

- Static catalog output remains deterministic.
- Dynamic evidence stays additive sidecar evidence.
- Dynamic evidence does not create or rewrite static `ScenarioPlan`s.
- Direct runtime `inputVariantId` remains the only exact evidence path.
- Fallback without direct id can at most produce `MATCHED_HIGH_CONFIDENCE`.
- Same-feature sibling ambiguity must remain visible.
- Owners are eligibility constraints, not proof by themselves.
- Fixture/prerequisite inputs remain visible for future executor/materialization work.
- Quizzes dynamic validation uses `SPRING_PROFILES_ACTIVE=test,sagas,local`.

## Do not change unless a focused test requires it

- `ConflictGraphBuilder`
- `ConnectedSagaSetEnumerator`
- `ScheduleEnumerator`
- `ScenarioSpaceAccountingCalculator`
- segment-compression code
- scenario executor POC classes
- Quizzes production code
- simulator messaging/transaction behavior unrelated to dynamic evidence
- Docker Compose defaults

---

# Phase 1 — unmatched reason diagnostics

## Build

Add reason classification for `UNMATCHED` records.

Record output:

```json
"dynamicEvidence": {
  "joinStatus": "UNMATCHED",
  "unmatchedReason": "HELPER_OWNER_MISMATCH"
}
```

Report/manifest counts:

```json
"unmatchedReasonCounts": {
  "FAILED_TEST_CLASS": 0,
  "NOT_SELECTED_TEST_CLASS": 0,
  "HELPER_OWNER_MISMATCH": 0,
  "UNCLASSIFIED": 0
}
```

Priority order:

1. `FAILED_TEST_CLASS`
2. `NOT_SELECTED_TEST_CLASS`
3. `HELPER_OWNER_MISMATCH`
4. `UNCLASSIFIED`

Classification rules:

- `FAILED_TEST_CLASS`: source/owner class has test-run status `FAILED`, `TIMED_OUT`, or `NO_REPORT`.
- `NOT_SELECTED_TEST_CLASS`: no source/owner class is in `selectedTestClassFqns`.
- `HELPER_OWNER_MISMATCH`: selected non-failed input has same-class same-saga/step evidence, but runtime feature method differs from static owner/provenance method and source method is helper/fixture-derived.
- `UNCLASSIFIED`: remaining unmatched record.

Prefer metadata-based helper detection. Do not hard-code Quizzes helper names as the only mechanism.

## Likely files

- `verifiers/.../dynamic/model/DynamicEvidenceSummary.java`
- add `verifiers/.../dynamic/model/UnmatchedReason.java`
- `verifiers/.../dynamic/model/DynamicEvidenceJoinResult.java`
- `verifiers/.../dynamic/DynamicEvidenceJoiner.java`
- add `verifiers/.../dynamic/UnmatchedReasonClassifier.java`
- `verifiers/.../dynamic/DynamicEnrichmentOrchestrator.java`
- `verifiers/.../dynamic/export/EnrichedScenarioCatalogWriter.java`

## TDD

Add/extend:

- `DynamicEvidenceJoinerSpec.groovy`
- `DummyappDynamicEnrichmentIntegrationSpec.groovy`
- `DynamicEnrichmentOrchestratorSpec.groovy`

Tests:

1. Unmatched input from failed class => `FAILED_TEST_CLASS`.
2. Unmatched input from non-selected class => `NOT_SELECTED_TEST_CLASS`.
3. Helper source, selected class, same saga/step evidence, different runtime feature => `HELPER_OWNER_MISMATCH`.
4. Failed class wins over helper mismatch.
5. Remaining unmatched => `UNCLASSIFIED`.
6. Enriched JSON contains `dynamicEvidence.unmatchedReason`.
7. Manifest/join report contains `unmatchedReasonCounts`.

## Validate

From `verifiers/`:

```bash
mvn -q -Dtest=DynamicEvidenceJoinerSpec test
mvn -q -Dtest=DummyappDynamicEnrichmentIntegrationSpec test
mvn -q -Dtest=DynamicEnrichmentOrchestratorSpec test
```

---

# Phase 2 — fixture/input role metadata

## Build

Add additive metadata to static inputs and exports:

```text
inputRole
fixtureOrigin
callContextMethodName
```

Expected mappings:

- direct feature body facade/saga call => `FEATURE_UNDER_TEST`, `DIRECT_FEATURE`
- direct `setup()` facade/saga call => `FIXTURE_PREREQUISITE`, `SETUP`
- helper facade/saga call reached from `setup()` => `FIXTURE_PREREQUISITE`, `SETUP_HELPER`
- field initializer => `FIXTURE_PREREQUISITE`, `FIELD`
- `setupSpec()` => `FIXTURE_PREREQUISITE`, `SETUP_SPEC`
- inherited fixture/helper variants use corresponding `INHERITED_*` origin.

Keep existing provenance fields:

```text
sourceClassFqn
sourceMethodName
sourceBindingName
provenanceText
```

Add call-context metadata instead of replacing `sourceMethodName`.

## Dummyapp fixture

Add a small dummyapp spec, likely:

```text
applications/dummyapp/src/test/groovy/com/example/dummyapp/GroovySetupHelperOwnershipSpec.groovy
```

Required shapes:

1. `setup()` calls a helper; helper calls a facade.
2. Two features depend on that setup.
3. Include one direct feature-body facade call for contrast.
4. Include same-feature sibling setup-helper calls only if needed for Phase 3 ambiguity tests.

## Likely files

- `verifiers/.../state/GroovyFullTraceResult.java`
- `verifiers/.../state/GroovyConstructorInputTrace.java`
- `verifiers/.../visitor/GroovyConstructorInputTraceVisitor.java`
- `verifiers/.../scenario/model/InputVariant.java`
- add model enums near `InputVariant`: `InputRole`, `FixtureOrigin`
- `verifiers/.../scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java`
- `verifiers/.../dynamic/DynamicInputMapWriter.java`

## TDD

Add/extend:

- `GroovyConstructorInputTraceVisitorDummyappSpec.groovy`
- `ApplicationAnalysisScenarioModelAdapterSpec.groovy`
- `DynamicInputMapWriterSpec.groovy`

Tests:

1. Setup-helper facade input preserves helper provenance.
2. Setup-helper facade input has `callContextMethodName=setup`.
3. Setup-helper facade input has `inputRole=FIXTURE_PREREQUISITE` and `fixtureOrigin=SETUP_HELPER`.
4. Setup-helper facade input owners include all feature methods that run that setup.
5. Direct feature call has `FEATURE_UNDER_TEST` and `DIRECT_FEATURE`.
6. Exported dynamic input map includes role/origin/call-context fields.
7. Deterministic input ids stay stable unless a focused test proves impossible.

## Validate

From `verifiers/`:

```bash
mvn -q -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec test
mvn -q -Dtest=ApplicationAnalysisScenarioModelAdapterSpec test
mvn -q -Dtest=DynamicInputMapWriterSpec test
```

---

# Phase 3 — setup/helper ownership in attribution

## Build

Use Phase 2 ownership metadata so setup-helper fixture inputs are eligible for the active Spock feature at runtime.

Target alignment:

```text
source/provenance: helper method
call context:      setup
owners:            feature methods that run setup
runtime identity:  active feature method
```

Expected outcomes:

- one eligible setup-helper input => runtime map can resolve it;
- one eligible setup-helper input without direct runtime id => joiner fallback can produce `MATCHED_HIGH_CONFIDENCE`;
- two same-feature same-saga setup-helper siblings => `AMBIGUOUS`, not exact;
- undeclared feature => no match.

## Likely files

- `GroovyConstructorInputTraceVisitor.java`
- `ApplicationAnalysisScenarioModelAdapter.java`
- `InputVariant.java`
- `DynamicInputMapWriter.java`
- `simulator/.../monitoring/dynamic/DynamicInputMap.java`
- `DynamicEvidenceJoiner.java`

## TDD

Add/extend:

- `DynamicInputMapTest.groovy` in `simulator/` if runtime map changes.
- `DynamicEvidenceJoinerSpec.groovy`
- `DummyappDynamicEnrichmentIntegrationSpec.groovy`
- Phase 2 static/export tests as needed.

Tests:

1. Runtime map resolves setup-helper input for feature A.
2. Runtime map resolves same multi-owner setup-helper input for feature B.
3. Runtime map does not resolve it for undeclared feature.
4. Runtime map returns ambiguous for two same-feature same-saga setup-helper siblings.
5. Joiner fallback returns `MATCHED_HIGH_CONFIDENCE` for one eligible setup-helper input and complete test identity.
6. Joiner fallback returns `AMBIGUOUS` for sibling setup-helper inputs.
7. Unrelated feature remains `UNMATCHED` with reason.
8. Dummyapp integration proves static catalog -> input map -> dynamic events -> enriched JSON.

## Validate

From `verifiers/`:

```bash
mvn -q -Dtest=GroovyConstructorInputTraceVisitorDummyappSpec,ApplicationAnalysisScenarioModelAdapterSpec test
mvn -q -Dtest=DynamicInputMapWriterSpec,DynamicEvidenceJoinerSpec,DummyappDynamicEnrichmentIntegrationSpec test
```

From `simulator/`, only if `DynamicInputMap` changes:

```bash
mvn -q -Dtest=DynamicInputMapTest test
```

---

# Final Quizzes validation

Run only after focused tests pass.

## Compose config

```bash
CATALOG_PATH=/dev/null docker compose config -q
```

## Dynamic baseline

Use 4 GiB for the verifier container. The original 2 GiB command is not reliable for this issue's current dynamic smoke: a 2026-06-30 attempt generated the static catalog (`584` scenarios) and was killed with exit code `137` before dynamic artifacts. Treat that as infrastructure/resource failure, not validation.

A later 4 GiB attempt produced artifacts but the verifier-side Maven process hit the configured timeout (`ProcessResult.timedOut=true`, `exitCode=-1`, `batchStatus=TIMED_OUT`, `mavenDurationMillis≈2361020`) and marked 42/45 test classes `TIMED_OUT`; treat that run as partial diagnostic output only, not comparable effectiveness validation.

For this plan to be complete, the Quizzes smoke must finish without verifier process timeout and without broad test-class timeout/no-report statuses. Do not mark this issue complete on targeted tests alone if this final effectiveness validation is still partial.

Use the same comparable shape as the 2026-06-29 baseline except for the required 4 GiB memory limit:

```bash
CATALOG_PATH=/dev/null \
MEDIUM_MEM_LIMIT=4g \
MEDIUM_MEM_RESERVATION=2g \
MEDIUM_CPUS=2 \
docker compose run --rm -T \
  -e SPRING_PROFILES_ACTIVE=test,sagas,local \
  -e VERIFIERS_APPLICATION_BASE_DIR=quizzes \
  -e VERIFIERS_OUTPUT_ROOT=/reports/unmatched-fixture-diagnostics-dynamic-smoke \
  -e VERIFIERS_SCENARIO_CATALOG_ENABLED=true \
  -e VERIFIERS_SCENARIO_CATALOG_CATALOG_WRITE_MODE=WRITE_PLANS \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_SAGA_SET_SIZE=1 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_CATALOG_SCENARIOS=2000 \
  -e VERIFIERS_SCENARIO_CATALOG_MAX_INPUT_VARIANTS_PER_SAGA=100000 \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ENABLED=true \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_ALLOW_PARTIAL_TEST_RUN=true \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_INCLUDE_TEST_DIRS=pt/ulisboa/tecnico/socialsoftware/quizzes/sagas \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_EXCLUDE_TEST_DIRS=pt/ulisboa/tecnico/socialsoftware/quizzes/causal,pt/ulisboa/tecnico/socialsoftware/quizzes/tcc \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_EXCLUDE_TEST_CLASSES=CreateTournamentDynamicEvidenceSmokeTest,DynamicEvidenceDisabledSmokeTest \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_PER_TEST_TIMEOUT_SECONDS=1800 \
  -e VERIFIERS_DYNAMIC_ENRICHMENT_MAVEN_PROFILE=test-sagas \
  fault-analysis-scenario-gen
```

Do not use the old `sagas,local` mis-profiled baseline.

## Compare metrics

Compare to:

```text
MATCHED_EXACT: 291
MATCHED_HIGH_CONFIDENCE: 109
AMBIGUOUS: 0
UNMATCHED: 184
```

Record:

```text
scenario records
MATCHED_EXACT
MATCHED_HIGH_CONFIDENCE
AMBIGUOUS
UNMATCHED
unmatchedReasonCounts
representative examples per unmatched reason
```

Success criteria:

- `UNMATCHED` has useful reason counts.
- Fixture role/origin appears in static/exported artifacts.
- `HELPER_OWNER_MISMATCH` decreases substantially or becomes honest `AMBIGUOUS`/matched statuses.
- Same-feature sibling setup calls do not become false exact matches.
- Failed/not-selected classes remain visible as caveats.

## Result artifact for later interpretation

After the final run, preserve the run path and a short summary of remaining unmatched/ambiguous examples. Use that evidence later to decide the next separate plan.

---

# Documentation after validation

Update current docs only:

- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/evidence.md`
- `docs/verifiers-impl/reference/dynamic-enrichment.md`
- `docs/verifiers-impl/verifier-pipeline-plain-explanation.md`
- `docs/verifiers-impl/glossary.md`
- optionally `docs/verifiers-impl/roadmap.md`

Document:

- provenance vs owner vs call context;
- input role / fixture origin;
- unmatched reason counts;
- Quizzes before/after;
- remaining unmatched/ambiguous examples for later interpretation.

# Suggested commits

1. `test: cover unmatched reason diagnostics`
2. `feat: classify unmatched dynamic evidence reasons`
3. `test: cover fixture roles for setup helper inputs`
4. `feat: propagate setup helper fixture ownership`
5. `docs: document unmatched dynamic diagnostics`
6. optional `docs: record Quizzes unmatched diagnostics smoke`
