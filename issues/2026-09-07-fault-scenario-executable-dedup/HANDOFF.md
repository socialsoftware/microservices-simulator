# FaultScenario executable-content deduplication handoff

State: complete.

## Outcome

`OnDemandFaultScenarioService` now reuses an exact compact executable match before
adding a generated FaultScenario. Equality includes WorkloadPlan, vector and the full
ordered action array and excludes only `id`. Existing IDs and duplicate historical
records remain untouched; the lowest retained matching ID is selected deterministically.
Exact repeated requests return the IDs recorded by their persisted request record.

The fix is request-local. It does not change `ScenarioIdGenerator`, the package schema,
writer/reader compatibility, execution semantics, ImpactV2 or the recovered-creation
score policy.

## Files and decisions

- Changed the on-demand merge in `OnDemandFaultScenarioService.java`.
- Added positive and negative Spock coverage in `OnDemandFaultScenarioServiceSpec.groovy`.
- Updated the on-demand current-state contract, Outcome 6 readiness, and the originating
  space-map finding. This issue contains the approved bounded spec and plan.
- The writer/reader mismatch was confirmed before the fix: equal compact records had
  unequal regenerated IDs and the old service added one duplicate.
- Compact equality is sufficient because the WorkloadPlan identity owns setup,
  participants, slots, event origins and recovery/semantic-lock facts. No second global
  canonical identity or artifact migration was introduced.

## Proof

- Java 21, isolated source copy and private Maven repository:
  `OnDemandFaultScenarioServiceSpec` plus `CurrentExecutableArtifactContractSpec` —
  58 tests, zero failures/errors/skips.
- Regression cases cover eager then demand, repeat demand, lowest-ID selection across
  retained duplicates, distinct recovery action orders, participant/slot order and event
  order.
- A copied compatible Quizzes package was requested for workload
  `8d4c46bcb8700b2fd70e4911d4a5730f6d374a185c2b307ef629d3b5f55dccf0`, vector
  `000000000`, cap 10000. The first result was `PERSISTED`, added zero scenarios and
  returned eager ID `b2cd97c18a849a63fb0e56e2725ef3ebf365e6f55ec5d0697f82ae9fbddecd16`;
  the repeat was `DEDUPLICATED` with the same ID. Fault rows stayed at 11,086 and one
  request row was written. The service validated the staged and final package revision.
- SHA-256 comparison confirmed every file in the original source package remained
  byte-identical. Only the private copied package was mutated.

## Known exclusions

Frozen packages retain their old duplicate IDs and analyses must continue to group them
explicitly. This prevents future duplicate additions; it is not a historical migration.
No replay campaign was rerun because persisted executable content and executor behavior
did not change.
