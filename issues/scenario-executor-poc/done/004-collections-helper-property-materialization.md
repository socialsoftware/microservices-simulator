## Parent PRD

`issues/scenario-executor-poc/prd.md`

## Type

AFK

## What to build

Extend materialization for collection and reduced-recipe shapes needed by executor-ready inputs. Support list, set, and map collections; supported `local_transform` for `toSet`; `helper_result` through its nested result recipe; and `property_access` when the receiver has been materialized. Reject unsupported call results, source-provided placeholders without values, unsupported transforms, and non-whitelisted unresolved values with structured blockers.

## Acceptance criteria

- [x] List, set, and map collection recipes materialize to runtime collection values with deterministic element/entry ordering semantics.
- [x] A supported `toSet` local transform over a materialized receiver produces a set value.
- [x] `helper_result` materializes through its nested result recipe without invoking Spock helper methods.
- [x] `property_access` reads a property or accessor value only when its receiver recipe has been materialized.
- [x] A `property_access` with an unmaterializable receiver is rejected with a structured receiver blocker.
- [x] Generic unsupported `call_result` values are rejected unless they are one of the explicitly whitelisted runtime-owned arguments from the PRD.
- [x] Source-provided placeholders without configured values are rejected.
- [x] Unsupported transforms and non-whitelisted unresolved values are rejected with stable blocker reasons.
- [x] The fixture execution path can consume at least one materialized collection/helper/property value as a constructor argument.

## Feature criteria covered

- AC-010
- AC-014
- AC-015
- AC-022

## Domain context

Preserve the public recipe node meanings from `docs/verifiers-impl/structured-input-recipes.md`: `collection`, `local_transform`, `helper_result`, `property_access`, `placeholder`, `call_result`, and `unresolved`.

## Verification plan

- Run focused verifier tests for collection/helper/property materialization and blocker output.
- Suggested command: `cd verifiers && mvn test -Dtest='*Materializ*Spec,*ScenarioExecutor*Spec' -DfailIfNoTests=false`

## Completion evidence

- Extended `ScenarioMaterializer` with deterministic list/set/map materialization, `toSet` local transform support, `helper_result` reduction through nested result recipes, and materialized-receiver `property_access`.
- Added stable blockers for unmaterializable property receivers, unsupported `call_result`, source-provided placeholders without configured values, unsupported transforms, and non-whitelisted unresolved values.
- Fixture execution consumes helper/collection/property-derived values through the public executor path.

## Verification

- Verified by `ScenarioExecutorSpec` collection/helper/property execution and blocker scenarios.
- Verified command: `cd verifiers && mvn test -Dtest='*ScenarioExecutor*Spec' -DfailIfNoTests=false` on 2026-05-26: 10 tests run, 0 failures, 0 errors.

## Blocked by

- Blocked by `issues/scenario-executor-poc/003-dto-constructor-materialization.md`

## User stories addressed

- User story 20
- User story 21
- User story 22
- User story 23
- User story 28
- User story 49
