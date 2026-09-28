# Complete-catalogue live search integration

The ordinary fixed-workload runner now accepts a complete catalogue for both GA and
uniform random. `run.py catalogue` enumerates the canonical vectors through the existing
Java generator, checks every request for completeness and seals the package/evidence.
`run.py run --catalogue ...` uses the same candidate structure and search loop as the
recorded-feedback experiments, calling the ordinary isolated executor for each selection.
No search operators, weights, application behavior or detector predicates changed.

The reusable candidate adapter moved from `exhaustive_reference.py` to `catalogue.py`;
compatibility exports preserve experiment callers. The original command without
`--catalogue` retains its on-demand two-stage sampling policy. Both paths remain explicit
in results; the new path does not silently reinterpret historical configurations.

## Verification

- 60 Python tests cover live command wiring, uniform draws independent of feedback,
  complete exhaustion, unknown feedback, unqualified controls, catalogue provenance,
  truncation and failed generation, plus existing runtime/fitness/search checks.
- All 120 retained GA traces match after extraction: original and uniform-exploration GA,
  30 seeds each on the 186- and 72-case maps. Candidate choices, scores, parents, operators,
  genes, recovery replacement, duplicates and stop reasons match.
- The ordinary catalogue command independently regenerates the 72-case AddParticipant →
  LeaveTournament → RemoveTournament catalogue (36 vectors). Preparation takes 58.79
  seconds, of which 55.20 are generator process time; no truncation.
- A bounded live integration uses seed 11, budget 12 per method, population 8, mutation
  0.3, stall limit 1000 and five unit weights. Each arm is sequential; the two independent
  arms run concurrently with isolated Docker attempts. The existing exact, successful,
  score-zero control is reused against the same source package and runtime.

The live pilot completed: **24 fresh application executions**, 12 per arm, all with
complete feedback. Both arms found five positives in this small sample. All choices,
component scores, parent decisions and execution status/conformance match the stored-map
replay exactly. GA proposed two duplicate children; neither was reexecuted. Both arms
stopped at budget and made zero generation requests during search. Each arm took about
388.66 seconds while running concurrently; this is integration timing, not an isolated
performance benchmark. The shared catalogue preparation cost is additional.

`validate.py` compares live choices, component scores, parent decisions and execution
status/conformance against the retained map using the same search policy. Its output is
`validation.json`. Raw commands, requests, packages and execution reports are under
`verifiers/target/catalogue-live-2026-09-16/`. These small runs validate the connection;
they are not a new effectiveness comparison or cluster performance measurement.

Uniform random uses successive seeded draws from sorted unseen candidates. This has the
same uniform-without-replacement distribution as the earlier shuffle baseline but does
not promise an identical permutation for the same numerical seed.

## Use and remaining scope

See the [ordinary command instructions](../../../../verifiers/experiments/fixed-workload-ga/README.md#complete-catalogue-live-search).
Reuse the catalogue across methods/seeds. Preparation time is reported separately from
per-search application and command time; an end-to-end evaluation must account for both.
Unknown outcomes consume budget without supplying fitness. The integration does not
change that policy, add cluster scheduling, or resume interrupted runs. The frozen local
runtime descriptor still references prepared files under `verifiers/target`; cluster
packaging and environment qualification remain separate work.
