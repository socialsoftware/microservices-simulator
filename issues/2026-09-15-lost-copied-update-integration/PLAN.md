# Lost copied update integration plan

## Environment and execution mode

The user approved this plan and the adjacent SPEC on 15 September 2026. Execution uses
parallel bounded implementation slices and a focused integration review. Work in the current local checkout,
preserving inherited dirty work. No new worktree, commit, push or merge. Use the supported
Docker runtime for application qualification and dummyapp-first generic tests. Source edits
start after the assembled package is approved, as required by the root AGENTS.md workflow.

## Implementation strategy

Reuse the proved inference and evidence chain, not the experimental scripts as a production
API. Keep source extraction, runtime observation, assessment and fitness as separate pieces.
Use native simulator hooks for gateway/registration boundaries where this avoids duplicating
the existing evidence infrastructure; retain narrowly targeted constructor instrumentation.
Package that instrumentation for the normal executor worker launch, with no runtime
self-attachment or Quizzes-specific class lists. Record unavailable instrumentation clearly.

Add a versioned optional copy-contract artifact with source provenance without invalidating
existing v4 packages. Absent contracts remain unavailable for this criterion. If the current
export/reader contract cannot support an additive artifact, amend this compatibility design
before changing package requirements. The old four-weight policy remains frozen; an explicit
new policy accepts five weights and uses the same per-criterion availability rules.

## M0 — Integration seams and inference

Outcome: supported copies are inferred through ordinary analysis and exported reproducibly
(FR-1). Inspect the visitor/state/export boundaries and Java-agent packaging/class-loading
order before edits. No hand-written class/field registry.

Anchors: `verifiers/experiments/inferred-stale-write/ExtractCopies.java`, the analysis state,
and scenario package writer/reader. Exercise dummyapp unrelated names, computed copies,
missing identities, deterministic ordering and round-trip compatibility with old packages.

## M1 — Attempt-scoped runtime evidence

Outcome: ordinary ScenarioExecutor attempts record the full supported chain (FR-2–6).
Boundaries: simulator local gateway/service, registration/commit observers, targeted
constructor instrumentation and verifier collector lifecycle. Keep Quizzes source intact.

Anchors: experimental `Agent.java`/`Probe.java`, `ImpactWriterContext`, `ScenarioExecutor`
and existing impact/read collectors. Replace process-global identity maps with an isolated
attempt session; bracket nested calls safely and clean up in finally paths. Verify both
transport modes and detect unsupported concurrent execution rather than sharing mutable
indexes across attempts. Start after setup and retain no cross-attempt DTO origins.

Proof: reused versus cloned/changed DTOs, reordered collections, duplicate aliases/keys,
ignored constructed objects, failed calls/transactions, observer exceptions and repeated
attempts. Missing-origin candidates must be observable as gaps rather than silently skipped.

## M2 — Assessment, report and configurable count

Outcome: grouped findings, coverage and the fifth criterion enter normal reports and search
(FR-4–10). Port the proven generic matching rule with full identity joins. Do not treat
matching scalar values or revision changes as sufficient evidence.

Anchors: experimental `assess.py`, ordinary executor assessment/report writers, and
`verifiers/experiments/fixed-workload-ga/fitness.py` plus worker/report ingestion and rescore.

Proof: forward/recovery positives and controls; multiple fields in one commit count once;
separate overwrites count separately; unrelated-field writes stay negative; deleting each
required evidence link prevents classification. Test unavailable versus disabled criteria,
old four-weight compatibility and five-weight preferences affecting GA parent selection.
Random selection remains unchanged. No large search campaign in this milestone.

## M3 — Application qualification and documentation

Outcome: reproduce the ten known executions through integrated observation, then qualify
source-derived setup and a generated positive workload (FR-11). Inspect current generated
schedules first; add a normal application test only if necessary for the input history.
Do not substitute a manually assembled workload for generated-case acceptance.

Compare business state/writes and old I/read findings with the frozen proof, accounting for
wall-clock audit fields. Record execution duration and evidence size with/without observation
as a small overhead check, not a broad performance claim. Record any generation obstacle
separately rather than absorbing a new search/generator feature into this package.

Update `docs/verifiers-impl/current-state.md`, its glossary, `roadmap.md`, the research brief
and issue HANDOFF with shipped scope, commands and measured results. Explain one positive
and one fresh-input control in plain words for the user. Stop before GA evaluation/RL.

## Validation strategy

Run focused module tests for inference, observer lifecycle, generic assessment and fitness.
Use the existing five-history/two-transport experiment as a compatibility oracle while
replacing only observation/assessment plumbing; then run the ordinary generated workload.
Keep report schema/version and artifact hashes reproducible. Complete the narrow relevant
regressions once; expand only for a failure or newly exposed boundary.

## Risks and fallbacks

- Hooks loaded too late: detect missing installation, make the criterion unavailable and
  fix the worker launch; never infer a zero from an empty trace.
- Lost object origins or ambiguous transport keys: retain explicit gaps and evidence;
  do not add value-only guesses to improve coverage.
- Untracked origin versus legitimate fresh input: test both; complete coverage is relative
  to the declared observation scope and must not imply an arbitrary lost-update negative.
- Instrumentation changes behavior: preserve original exceptions, compare committed
  business observations, and treat mismatches as a release blocker for this criterion.
- Generated positive requires wider generator changes: report the exact missing capability
  and ask for the scope decision; controlled positives alone do not satisfy FR-11.
- Overhead dominates execution: measure before broad search; keep instrumentation opt-in
  and review retention cost without silently dropping required evidence.
