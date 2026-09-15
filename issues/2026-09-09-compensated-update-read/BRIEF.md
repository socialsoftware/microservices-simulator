# Compensated-update read diagnostic

Approved in conversation on 9 September after the four-history Tournament experiment
and explanation of the detection rule. The user explicitly requested implementation and
verification. Execute as a reviewed direct change in the current checkout; preserve
existing dirty documentation and personal notes. No further implementation gate is pending.

## Outcome and boundary

Extend the existing opt-in Saga/local read diagnostic from compensated creations to
reads of existing-object updates whose effects are subsequently wholly or partly restored
by explicit compensation of the exact producing step occurrence. Use the existing typed
outer-response adapters and committed-write/baseline instrumentation. No arbitrary log
parsing, new business invariant, DTO-field guessing, application repair, or score weighting.

The proof requires: exact producer revision and predecessor; a different Saga receives
that revision before producer completion; ordered same-producer explicit compensation
with the exact source occurrence/checkpoint; at least one covered application attribute
changed by the forward update and restored by compensation. An intervening writer or
ambiguous/missing evidence is unknown. Failed reads and reads before update/after recovery
are not positive witnesses. Preserve existing reader-outcome semantics: reader success,
read-only behavior or subsequent recovery do not erase an exposure already proven.

Use deterministic fingerprints of top-level persistent application attributes from the
existing projections, avoiding raw application values in the sidecar. Compare whole
structured attribute values atomically for this bounded slice; do not invent list element
identities or recursively guess DTO mappings. Report which changed attributes were
restored and which were not. A revision-only/semantic-lock-only change is insufficient.
A fingerprint is an equality witness, not a claim that B used or received every field.
Generic wording is revision exposure; a particular returned-field claim still requires
an audited response mapping/application evidence.

Deduplicate by producer/reader/produced revision, with explicit creation/update categories.
Keep the count separate from ImpactV1, ImpactV2's persistent-object union and GA fitness.
Version the sidecar contract if required; document compatibility and historical evidence.
No extension to internal reads, collections/predicates, nested DTOs, event consumers,
transport topologies, runtime input binding, lost updates or other anomaly families.

## Execution and acceptance

1. Sol/medium implements the generic collector/report/assessor and dummyapp-oriented
   Spock tests; Astra integrates runtime evidence and documentation and reviews the diff.
2. Positive and negative/unknown tests cover partial restoration, no restoration,
   metadata-only effects, writer interference, missing predecessor/projection/checkpoint,
   exact revision identity, repeated reads and existing creation behavior.
3. Repeat the four real UpdateTournament/FindTournament histories in both serialization
   modes through the production diagnostic, retaining exact action/source contracts.
   Verify ordinary ScenarioExecutor sidecar persistence with the new schema as well.
4. Run narrow meaningful regressions and review; update current-state, glossary, roadmap,
   experiment instructions and final handoff with actual evidence and exclusions.

The existing topic course-ID loss during Tournament recovery remains a separate follow-up.
This implementation must report partial restoration correctly without repairing the app
or claiming that a zero anomaly count certifies the final state's correctness.
