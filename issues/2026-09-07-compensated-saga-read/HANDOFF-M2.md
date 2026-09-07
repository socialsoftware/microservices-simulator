# M2 handoff — Quizzes qualification

State: **complete**. Final independent review: **PASS**, with no in-scope blockers.
Implementation, runtime audit and durable artifact review passed.

## Outcome and actual changes

FR-9 and the application coverage boundary are implemented. Quizzes supplies explicit
Saga/local adapters for the singular QuizDto and TournamentDto outer revisions. The new
seven-test Spock suite verifies persistent identity, revision provenance, nested exclusion
and Saga/local profile registration.

Actual implementation files:

- `applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/diagnostics/QuizzesSagaReadResponseAdapters.java`.
- `applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/diagnostics/QuizzesSagaReadResponseAdaptersTest.groovy`.
- `verifiers/experiments/saga-read-exposure/`: controlled Java driver, ordinary CLI clock wrapper, private Docker build/qualification runner, auditor and README.
- This issue's `M2-PROTOCOL.md`, `RESULTS.md`, SPEC/PLAN status and handoff; canonical current-state and roadmap.

The five controlled cases use real workflow APIs, injected boundaries, recovery checkpoints
and production collectors/assessor. The harness binds B to A's actual created ID and
reports its controlled source contract honestly. The ordinary executor separately reads a
setup-backed Quiz and persists its real sidecar. No generated runtime binding, score,
service, DTO, semantic-lock or production dependency change was introduced.

## Discovery and autonomous decisions

Persistent logical types are SagaQuiz/SagaTournament, matching actual framework identities;
no prefix stripping is used. Real source recovery consists of explicit generateQuizStep
compensation and implicit getTopicsStep/getCourseExecutionStep rollback. Source contract,
author FQNs and action/checkpoint occurrences are checked against these real outcomes.
B startup, probes and setup are excluded from observed application reads.

A fixed DateHandler.now Mockito fixture keeps application dates equal in both modes and
preserves other DateHandler methods; JOL remains an experimental Java agent. The protocol
records the fixed clock and fresh-process timing limitations before measurements. Assessment
and final-horizon timings are separate from actions. The reviewer requested a real successful
ordinary delivery control, not merely equal zero reports; that check passed.

The first auditor treated application logs as source additions and retained volatile setup
metadata in ordinary equivalence. Its failing report is preserved. Independent rehashing
proved all measured sources/binaries/packages unchanged and supported two narrow audit-only
corrections described in RESULTS. No runtime sample was discarded or Java source patched.
These are necessary implementation/proof details within the approved boundary, not scope
expansions. The original linear-cost hypothesis was not established; indexed joins still
include per-call history/action scans, as already recorded after M1.

## Verification

Docker build and adapter suite passed; **22 fresh-JVM Docker runs and 497 final audit checks
passed**, with 15 full paired execution/ImpactV1/ImpactV2 report comparisons. Positives prove
creation→exact delivery→same-step direct-predecessor compensated deletion, including B
finishing read-only. Controls cover failed calls, successful overlapping A, outer Tournament
and real ordinary SUCCESS/EXACT FindQuiz. All enabled scopes are complete with zero gaps.

The continued independent reviewer passed adapter/driver preflight and accepted the audit
corrections after rehashing 983 source files, 1780 runtime artifacts and nine package files.
Final review independently verified the 227 archive entries, 161 run-artifact hashes,
all 15 paired report comparisons and all 17 enabled sidecars. The final 497 checks passed;
the first failing summary and original completion remain preserved. Final results, every
cost sample, memory interpretation, limits, hashes and raw archive location are in
[RESULTS.md](RESULTS.md). Root independently inspected the actual XML,
source/binary hashes, findings, persistent references and ordinary baseline delivery.

## What to try

Use the tracked experiment README to run the fixed matrix from an independent directory,
or inspect `verifiers/target/saga-read-exposure/m2/qualification-01/summary.json` and the
reader-only/ordinary enabled sidecars. For an already executable Saga/local package, append
`--microservices.simulator.saga-read-exposure.enabled=true` to its normal ScenarioExecutor
CLI invocation, keeping ImpactV2 write observation enabled. The sidecar is additive.

No user decision is pending. Broader read provenance, restored updates, runtime binding,
scaling/retention policy and business-harm scoring remain explicit future work. Preserve the
unrelated meeting note and concurrent space-map work; do not push, merge or create a PR.
