# Dynamic Evidence FQN Attribution Plan

## Goal

Make verifier dynamic evidence attribution use fully qualified saga class names (FQNs) as the canonical identity.

The concrete ambiguity to remove is duplicate simple saga class names, e.g.:

```text
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveCourseExecutionFunctionalitySagas
pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveCourseExecutionFunctionalitySagas
```

Runtime dynamic evidence already emits the needed FQN fields:

```json
{
  "functionalityName": "RemoveCourseExecutionFunctionalitySagas",
  "functionalityClassFqn": "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveCourseExecutionFunctionalitySagas",
  "functionalityClassSimpleName": "RemoveCourseExecutionFunctionalitySagas"
}
```

The verifier currently reads and matches primarily through `functionalityName`, which can overmatch duplicate simple names.

## Non-goals

Do not change runtime `functionalityName` to FQN. Runtime `functionalityName` is still a display/runtime behavior key used by logs, impairment/fault config, invocation IDs, and `UnitOfWork`.

Do not remove simple names from HTML/debug output. Simple names remain fine for presentation. They are not fine as canonical dynamic attribution identity.

Do not run a Quizzes smoke as part of the required validation for this change. This is a joiner/reader identity fix and should be proven with focused dynamic tests.

## Required Behavior

1. If an event has `functionalityClassFqn`, dynamic join must use that exact FQN.
2. If `functionalityClassFqn` is absent, legacy simple-name/functionality-name matching remains enabled.
3. If legacy fallback sees duplicate candidate FQNs for the same observed name, the result must be `AMBIGUOUS`, not silently matched to one FQN.
4. Existing unambiguous legacy simple-name evidence must continue to match.
5. Static scenario catalog output must not change. This task only changes dynamic evidence reading/joining.

## Implementation Steps

### 1. Extend verifier dynamic event model

Edit:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/model/DynamicEvidenceEvent.java
```

Add two record fields immediately after `functionalityName`:

```java
String functionalityClassFqn,
String functionalityClassSimpleName,
```

Do not add optional helper methods unless they remove duplicated code in the joiner. Keep the model simple.

After this edit, update every `new DynamicEvidenceEvent(...)` call in tests and source to pass the two new arguments.

Expected direct callers to update:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DynamicEvidenceReader.java
verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DynamicEvidenceJoinerSpec.groovy
verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DynamicEvidenceReaderSpec.groovy
```

If compilation reports more constructor calls, update them mechanically by passing `null, null` where the test intentionally models old evidence.

### 2. Read FQN fields from dynamic evidence JSON

Edit:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DynamicEvidenceReader.java
```

In `fromMap(...)`, construct `DynamicEvidenceEvent` with:

```java
text(node, "functionalityName"),
text(node, "functionalityClassFqn"),
text(node, "functionalityClassSimpleName"),
text(node, "functionalityInvocationId"),
```

Old JSON without these fields must continue to parse with `null` FQN/simple-name fields.

### 3. Make `DynamicEvidenceJoiner` FQN-first

Edit:

```text
verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DynamicEvidenceJoiner.java
```

Change event candidate saga resolution.

Current risky shape:

```java
Set<String> candidateSagaFqns = catalogIndex.matchingSagaFqns(event.functionalityName());
```

Replace it with FQN-first logic:

```java
Set<String> candidateSagaFqns = catalogIndex.matchingSagaFqns(event);
```

Implement this inside `CatalogIndex` or as a private joiner helper. Required semantics:

```java
if event.functionalityClassFqn is non-blank:
    return Set.of(event.functionalityClassFqn) only if that exact FQN exists in the catalog
    otherwise return Set.of()
else:
    return legacy alias matches by functionalityName
```

Do not use `functionalityClassSimpleName` for canonical matching. It is only a display/backcompat field.

### 4. Keep legacy duplicate fallback explicitly ambiguous

The existing joiner already has `DynamicEvidenceJoinStatus.AMBIGUOUS`. Preserve that behavior for simple-name-only duplicate evidence.

Do not implement fallback as “return empty set when duplicate”. That would turn ambiguous identity into generic `UNMATCHED` and lose useful diagnostics.

Required fallback semantics:

```java
if functionalityClassFqn is absent and functionalityName maps to one FQN:
    match that FQN as before

if functionalityClassFqn is absent and functionalityName maps to multiple FQNs:
    keep all candidates so the current join analysis reports AMBIGUOUS
```

### 5. Resolve observed records by exact FQN

Update `resolveSagaFqn(plan, event)`.

Current shape:

```java
return planSagaFqns(plan).stream()
    .filter(fqn -> sagaNameMatches(fqn, event.functionalityName()))
    .findFirst()
    .orElse(null);
```

Required shape:

```java
String eventFqn = nonBlank(event.functionalityClassFqn());
Set<String> planSagas = planSagaFqns(plan);
if (eventFqn != null && planSagas.contains(eventFqn)) {
    return eventFqn;
}

return legacySimpleNameResolveOnlyWhenUnambiguousWithinPlan(planSagas, event.functionalityName());
```

The fallback resolver must return `null` when multiple plan sagas match the same simple/functionality name.

This prevents observed commands/aggregate accesses from being labeled with the wrong duplicate saga FQN.

## Tests to Change

Use existing focused tests. Do not create a broad integration test for this.

### Primary test file

Edit:

```text
verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DynamicEvidenceJoinerSpec.groovy
```

Update its private `event(...)` helper to pass the new constructor fields:

```groovy
event.functionalityName as String,
event.functionalityClassFqn as String,
event.functionalityClassSimpleName as String,
event.functionalityInvocationId as String,
```

Then add these three tests.

#### Test 1: FQN disambiguates duplicate simple names

Create two plans sharing the same simple saga name:

```groovy
def executionFqn = 'com.example.execution.RemoveCourseExecutionFunctionalitySagas'
def tournamentFqn = 'com.example.tournament.RemoveCourseExecutionFunctionalitySagas'
def executionPlan = plan('scenario-execution', [input('input-execution', executionFqn)], executionFqn)
def tournamentPlan = plan('scenario-tournament', [input('input-tournament', tournamentFqn)], tournamentFqn)
```

Create an event with simple `functionalityName` but exact tournament FQN:

```groovy
event('STEP_STARTED', [
    testClassFqn: 'com.example.RemoveSpec',
    testMethodName: 'removes tournament course execution',
    functionalityName: 'RemoveCourseExecutionFunctionalitySagas',
    functionalityClassFqn: tournamentFqn,
    functionalityClassSimpleName: 'RemoveCourseExecutionFunctionalitySagas',
    stepName: 'reserve'
])
```

Expected result:

```groovy
result.records()*.scenarioPlanId() == ['scenario-execution', 'scenario-tournament']
result.records()*.dynamicEvidence()*.joinStatus() == [DynamicEvidenceJoinStatus.UNMATCHED, DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE]
result.records()[1].dynamicEvidence().observedSteps()[0].sagaFqn() == tournamentFqn
```

If the exact status is `MATCHED_PARTIAL` because test identity is incomplete in the final test shape, add test identity so it is `MATCHED_HIGH_CONFIDENCE`. Do not accept `AMBIGUOUS` for the FQN case.

#### Test 2: Simple-name-only duplicate remains ambiguous

Use the same two-plan setup, but omit `functionalityClassFqn` and `functionalityClassSimpleName`:

```groovy
event('STEP_STARTED', [
    testClassFqn: 'com.example.RemoveSpec',
    testMethodName: 'removes tournament course execution',
    functionalityName: 'RemoveCourseExecutionFunctionalitySagas',
    stepName: 'reserve'
])
```

Expected:

```groovy
result.records()*.dynamicEvidence()*.joinStatus() == [DynamicEvidenceJoinStatus.AMBIGUOUS, DynamicEvidenceJoinStatus.AMBIGUOUS]
```

This proves legacy duplicate evidence is no longer silently attributed to one FQN. If an existing duplicate-simple-name test already covers this, update it to use the `RemoveCourseExecutionFunctionalitySagas` naming and keep the explicit `AMBIGUOUS` assertion.

#### Test 3: Unambiguous legacy fallback still works

Use one plan only:

```groovy
def plan = plan('scenario-unique', [input('input-unique', 'com.example.UniqueFunctionalitySagas')], 'com.example.UniqueFunctionalitySagas')
```

Event has no FQN:

```groovy
event('STEP_STARTED', [
    testClassFqn: 'com.example.UniqueSpec',
    testMethodName: 'runs unique',
    functionalityName: 'UniqueFunctionalitySagas',
    stepName: 'reserve'
])
```

Expected:

```groovy
result.records()[0].dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.MATCHED_HIGH_CONFIDENCE
```

### Reader test file

Edit:

```text
verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/dynamic/DynamicEvidenceReaderSpec.groovy
```

Add or update a reader test that writes one JSONL event with:

```json
"functionalityName": "RemoveCourseExecutionFunctionalitySagas",
"functionalityClassFqn": "com.example.tournament.RemoveCourseExecutionFunctionalitySagas",
"functionalityClassSimpleName": "RemoveCourseExecutionFunctionalitySagas"
```

Expected:

```groovy
result.events()[0].functionalityName() == 'RemoveCourseExecutionFunctionalitySagas'
result.events()[0].functionalityClassFqn() == 'com.example.tournament.RemoveCourseExecutionFunctionalitySagas'
result.events()[0].functionalityClassSimpleName() == 'RemoveCourseExecutionFunctionalitySagas'
```

Also keep or add an old-format JSONL event without the two new fields and assert:

```groovy
result.events()[0].functionalityClassFqn() == null
result.events()[0].functionalityClassSimpleName() == null
```

## Required Validation

Run exactly this command from the repository root:

```bash
cd verifiers && mvn -q -Dtest=DynamicEvidenceJoinerSpec,DynamicEvidenceReaderSpec test
```

This command has been checked against the current project setup. The `verifiers/pom.xml` Surefire config includes `**/*Spec.class`, and `-Dtest=DynamicEvidenceJoinerSpec,DynamicEvidenceReaderSpec` runs the two focused Spock specs. Maven still compiles all test sources, so constructor signature breakage in other tests is caught at compile time.

Passing criteria:

1. Command exits with code 0.
2. `DynamicEvidenceJoinerSpec` includes and passes the three FQN/simple-name tests above.
3. `DynamicEvidenceReaderSpec` proves new fields are parsed and old records remain supported.
4. Existing `DynamicEvidenceJoinerSpec` baseline test `joins generated full baseline sized fixture through public outputs quickly` still passes unchanged:
   - `dynamicEventsRead() == 20_000`
   - `evidenceFilesRead() == 1`
   - exactly one `MATCHED_EXACT`
   - exactly 65 `UNMATCHED`

Do not run Quizzes smoke for this task. It is not required and is weaker evidence for this change than the focused joiner/reader tests.

## Expected Output Changes

Static scenario-generator output must not change:

- discovered saga count unchanged;
- accepted input count unchanged;
- missing saga input coverage unchanged;
- scenario catalog JSONL unchanged except if dynamic enrichment output is regenerated.

Dynamic enrichment behavior changes only for duplicate simple-name evidence:

- Events with `functionalityClassFqn` attribute to the exact FQN.
- Duplicate simple-name-only events remain `AMBIGUOUS` instead of being silently attributed.
- Unambiguous simple-name-only legacy events still match.

`CreateQuestionFunctionalitySagas` remains `NO_MATCH` if there is still no accepted static input variant for its exact FQN. This task does not improve static input coverage.
