# Scenario Catalog Follow-Up Prompt

Use this prompt to resume the scenario-catalog discussion in a future session.

## Prompt

You are working in the `microservices-simulator` repository on the `verifiers/` module scenario-catalog export. The previous session added a JSONL/manifest export for generated scenario plans and ran Quizzes single-saga smoke tests. Do not jump into implementation until the scope is clarified and approved.

Focus on the next design work around **test-derived input variants**, **transactional-mode filtering**, and **executor replay recipes**. Keep the existing implementation boundaries in mind: the current catalog is a machine-readable scenario contract, not the ScenarioExecutor itself.

## Current State To Assume

- The verifier can export:
  - `scenario-catalog.jsonl`
  - `scenario-catalog-manifest.json`
  - existing HTML report remains human-facing.
- The current generator supports single-saga scenarios and bounded generation.
- Quizzes single-saga smoke with high caps produced:
  - `65` saga classes adapted;
  - `1581` raw input traces;
  - `549` adapted input variants;
  - `537` accepted replayable variants under `RESOLVED_OR_REPLAYABLE`;
  - `12` partial variants excluded by default policy;
  - `25` saga classes with accepted inputs;
  - `40` saga classes without usable inputs;
  - no scenario cap/input cap hit.
- Allowing partial inputs produced `549` scenarios but did not add new saga classes.
- All expected aggregate footprint types were seen: Course, Execution, Question, Quiz, QuizAnswer, Topic, Tournament, User.
- `logicalKeyBindings` were empty for all Quizzes input variants in the smoke. This is a major limitation before trustworthy two-saga interleavings.

## User Decisions

### Decision 1: Prefer exhaustive input discovery

The user prefers treating every valid functionality call as a scenario input candidate, not only the top-level tested call.

Include candidates from:

- test method bodies;
- `setup()` methods;
- helper methods;
- nested helper calls.

Reason: the more valid input combinations the generator has, the better the later scenario executor/search pipeline can be. Missing valid setup/helper calls means missing useful scenario inputs.

Do not discard helper/setup calls just because they are fixture preparation. Instead, preserve them and label their role.

Suggested role labels:

```text
PRIMARY_TEST_SUBJECT
SETUP_CALL
HELPER_DERIVED_CALL
```

Default can eventually be exhaustive, while config can still filter roles for debugging or bounded runs.

### Decision 2: TCC/causal filtering must be generic, not Quizzes-specific

The user agrees that causal/TCC-origin inputs are suspicious for a saga-targeted catalog, but does not want a Quizzes-only package-name hack.

In Quizzes specifically:

- saga tests generally use/import/extend `BeanConfigurationSagas`;
- causal/TCC tests use/import/extend `BeanConfigurationCausal`;
- there are no `*TCC*.groovy` tests in the observed tree;
- one observed causal test was `TournamentFunctionalityCausalTest.groovy`;
- Quizzes reuses the same façade/functionality classes in both modes.

Important example pattern:

```java
switch (workflowType) {
  case SAGAS:
    new CreateUserFunctionalitySagas(...)
  case TCC:
    new CreateUserFunctionalityTCC(...)
}
```

Therefore, seeing a Groovy call such as:

```groovy
userFunctionalities.createUser(userDto)
```

is not enough. The analyzer needs to know which branch the source test would use.

Generic evidence to classify source mode should include:

- `SagaUnitOfWorkService` versus `CausalUnitOfWorkService`;
- `SagaCommandHandler` versus `CausalCommandHandler`;
- `Sagas*Factory` versus `Causal*Factory`;
- `*RepositorySagas` versus `*RepositoryTCC`;
- active profile/configuration evidence where statically available;
- branch labels in functionality creation sites, e.g. `case SAGAS`, `case TCC`;
- package/name hints only as low-confidence fallback.

Desired metadata shape:

```text
sourceMode: SAGAS | TCC | MIXED | UNKNOWN
confidence: CREATION_SITE_BRANCH | TEST_CONFIGURATION | TYPE_EVIDENCE | PACKAGE_HINT | UNKNOWN
evidence: human-readable evidence, e.g. BeanConfigurationSagas or CausalUnitOfWorkService
```

For a saga catalog, TCC/causal source inputs should either be rejected or accepted only with an explicit low-confidence/configured policy.

### Decision 3: Inputs should be replay instructions, not concrete values

The user confirmed that many values cannot be statically known because they come from runtime setup calls. This is expected.

The future ScenarioExecutor should replay setup recipes to obtain runtime values. Therefore, the catalog should not try to force everything into concrete primitive/DTO JSON values.

Current catalog provenance is useful but still too textual. Future work should move toward structured replay recipes.

Current-style provenance:

```text
arg[1]: courseExecutionDto
  <- createCourseExecution(...)
  <- courseExecutionFunctionalities.createCourseExecution(courseExecutionDto)
  [runtime edge].aggregateId
```

Desired future structure:

```json
{
  "setup": [
    {
      "id": "create-user-1",
      "call": "userFunctionalities.createUser",
      "args": [
        { "newDto": "UserDto", "fields": { "name": "Alice" } }
      ],
      "bindResultAs": "userCreatorDto"
    }
  ],
  "target": {
    "call": "tournamentFunctionalities.createTournament",
    "args": [
      { "from": "userCreatorDto.aggregateId" },
      { "from": "courseExecutionDto.aggregateId" },
      { "from": "topicIds" },
      { "from": "tournamentDto" }
    ]
  }
}
```

Think of an `InputVariant` as:

```text
InputVariant = replay plan + target saga binding
```

The executor should eventually:

1. run setup functionality calls in order;
2. retain returned DTOs/aggregate IDs;
3. bind those runtime values into target saga constructor inputs;
4. execute the target saga step by step.

## Deferred Work

Do not combine all of this into one implementation without a new brief/plan.

The recommended order for future sessions is:

1. **Mode classification**: prevent causal/TCC-origin inputs from contaminating a saga catalog.
2. **Helper promotion**: make setup/helper functionality calls become standalone input candidates.
3. **Structured replay recipe**: replace text provenance with executor-oriented recipe graphs.

Reason for this order: make the catalog semantically cleaner before expanding the number of candidate inputs, then make those inputs executable by the future ScenarioExecutor.

## Existing Note To Read First

Before doing new design or implementation, read:

```text
docs/verifiers-impl/test-analysis-saga-input-flow.md
```

That file contains the detailed plain-language explanation of the current static-analysis/test-analysis flow.

## What Not To Do

- Do not implement two-saga interleaving generation yet based only on broad type-level fallback and empty logical key bindings.
- Do not hardcode Quizzes-specific package filters as the primary solution.
- Do not treat causal/TCC-derived inputs as equivalent to saga-derived inputs without source-mode metadata.
- Do not require all input values to be statically concrete; runtime replay is part of the intended future executor design.
- Do not make the HTML report the machine contract.
