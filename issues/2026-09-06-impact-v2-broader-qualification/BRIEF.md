# Broader ImpactV2 qualification

The user approved broader qualification, prioritizing the historical 34-case benchmark.
This package evaluates the existing score; it does not change its meaning or repair
application defects.

## Runs and comparisons

1. Regenerate the current Quizzes catalogue with recovery cap 20. Resolve the historical
   provider-backed RemoveTournament/AddParticipant workload structurally and request its six missing
   canonical fault vectors through the ordinary request CLI. Preserve the historical
   34-row, 19/15 application-rule result as a separate baseline. Current generation has
   fewer recovery actions; report that change and map schedules only where justified.
2. Before runtime outcomes, freeze one materializable single-participant workload per
   eligible Saga. Prefer fewer events, shorter setup, then workload ID. Add the workload
   with most scheduled events for each eligible event-bearing Saga. Pair each selection
   with its zero vector and last-slot fault; choose the smallest FaultScenario ID when
   multiple recovery schedules exist. This gives 60 attempts, 30 pairs, 26 of 68 Saga
   types. The 42 excluded types have no eligible single-participant workload; this is
   not a claim that all 42 are inherently unexecutable.
3. Compile immutable copies once in Docker and run each attempt in a fresh JVM/H2
   application. At most two JVMs run concurrently. Retain logs, selection/package/source
   hashes, execution reports, old V1 and new V2 assessments, and benchmark observations.

A materializable setup is static eligibility, not proof that execution will succeed.
Retain failed controls and their fault attempts; do not replace selections after seeing
results. A zero-fault control is not an alternative serial execution and does not prove
serializability. The broader sample does not exhaust inputs, fault slots, schedules,
event routes or concurrent pairs.

## Acceptance evidence

Report attempted and completed counts separately, plus COMPLETE/PARTIAL/INVALID/
UNAVAILABLE. Never convert absent or incomplete evidence to zero. Validate report
identity and distinct-object scoring, retain category evidence, and compare each fault
attempt against its own control. Explain positive controls and deviations before
attributing anything to fault injection. An observed affected-object count measures
potential impact under the three implemented checks, not severity or universal domain
incorrectness.

The old Quizzes-specific broken-reference rule is comparison evidence only; it is
never an input to ImpactV2. Preserve both exact schedule matches and any justified
projection that removes obsolete recovery actions, without calling a projected mapping
an exact replay. Finish with independent review and update the canonical current-state
and roadmap. Source fixes, score changes, commits, merges and pushes are excluded.

## Qualification discovery

The historical descriptor/provider workload is the correct family for the retained
34-row baseline. The newer application benchmark wrapper instead requires the later
12-action automatic source setup. Its initial 17 selections were rejected before
application startup, producing no ScenarioExecutor or ImpactV2 reports. Retain these
wrapper rejections separately, then execute all 17 current scenarios through the ordinary
ScenarioExecutor with the same frozen build and original provider. This bounded harness
route change does not change the score or the application. The old rule labels remain
post-assessment historical comparisons; no new independent application-rule observation
is claimed unless separately evidenced.
