# 008 - Document Segment Compression Decision

Mode: AFK
Parent PRD: ./prd.md
Blocked by: 007-quizzes-count-only-comparison.md
Feature Criteria Covered: AC-20, AC-21
Verification Mode: review
Proof Required: documentation diff/read evidence showing placeholder language removed or superseded, glossary/current-state terms updated, scenario-space-accounting deferred note superseded, and a decision record added or updated

## Slice Contract

Update verifier documentation and decision records after implementation and Quizzes evidence exist.

Replace placeholder/deferred `SEGMENT_COMPRESSED` language with real conflict-anchor segment-compression semantics and honest limitations. Preserve claim safety: segment compression is static schedule-space reduction under verifier conflict evidence, not proof of semantic completeness, exact runtime binding, execution, fault injection, impact scoring, GA, or bandit behavior.

## Acceptance Criteria

- `docs/verifiers-impl/current-state.md` no longer claims `SEGMENT_COMPRESSED` is a placeholder, deferred strategy, small-tuple OPI behavior, or large-tuple serial fallback after implementation is complete.
- Glossary/current-state documentation defines or references `conflict anchor`, `anchor segment`, and segment-compressed scheduling in project terminology.
- Documentation explicitly states that segment compression preserves conflict-anchor order cases under static conflict evidence while collapsing non-anchor/internal step permutations.
- Documentation explicitly states that interaction pruning remains separate from segment compression.
- The scenario-space-accounting PRD deferred note or historical AC-14 caveat is superseded or linked from current docs so readers understand this package replaced that limitation.
- A verifier decision record is added or updated explaining why conflict-anchor segment compression was chosen over full step-level interleaving, serial fallback, and pairwise-orientation-only compression.
- Documentation preserves limitations around incomplete exact aggregate-instance binding, static conflict evidence, no runtime impact proof, no executor changes, no fault injection, no GA, and no bandit scope.
- Documentation references dummyapp and Quizzes verification evidence from the completed implementation slices where appropriate.

## Domain Context

- `current-state.md` is the present-tense source of truth for implemented verifier behavior.
- Decision records explain durable tradeoffs and should be added only when the decision is hard enough to reverse, surprising without context, and based on real alternatives.
- The selected compression model affects thesis claims, accounting baselines, and future schedule semantics.

## Implementation Notes

- Update docs only after prior slices prove the behavior with tests and Quizzes count-only evidence.
- Do not overstate dynamic/runtime guarantees. Dynamic enrichment remains sidecar evidence and execution/search stages remain separate roadmap work.
- Keep documentation concise and aligned with the glossary language used in the PRD.

## Completion Evidence

- Implementation: Updated `docs/verifiers-impl/current-state.md` to replace the present-tense `SEGMENT_COMPRESSED` placeholder warning with implemented conflict-anchor segment-compression semantics, strict/broad conflict-lens behavior, separation from interaction pruning, zero/all/mixed-anchor behavior, accounting parity, limitations, and dummyapp/Quizzes validation evidence.
- Implementation: Updated `docs/verifiers-impl/glossary.md` to define `conflict anchor`, `anchor segment`, and `segment-compressed scheduling`, and to update the `Schedule strategy` entry.
- Implementation: Updated `docs/verifiers-impl/roadmap.md` to mark conflict-anchor segment-compressed scheduling/accounting as an implemented static reduction while preserving exact-key/runtime-completeness limitations.
- Implementation: Added decision record `docs/verifiers-impl/decisions/2026-06-16-conflict-anchor-segment-compression.md` and linked it from `docs/verifiers-impl/decisions/index.md`. The decision record documents the chosen model, rejected alternatives (full step-level interleaving, serial fallback, pairwise-orientation-only compression, and runtime schedule redefinition), consequences, limitations, and verification evidence.
- Verification: PASS - doc grep confirmed stale present-tense placeholder language is removed from current-state/glossary/roadmap; remaining placeholder/serial-fallback mention is historical context in the new decision record. Grep also confirmed current docs include conflict-anchor/anchor-segment terminology, scenario-space-accounting PRD caveat supersession, and Quizzes count-only evidence (`218528454 -> 1019393`).
- Slice compliance review: PASS (`ses_1317678b5ffeB9koyJxr89a6Bw`) - confirmed current-state/glossary/roadmap/decision docs satisfy the issue acceptance criteria, limitations are preserved, Quizzes evidence/artifacts match documented totals, and historical meeting-note stale language is non-blocking because current-state is canonical.
- Code quality review: PASS (`ses_131767884ffeCL6YrhLpML5I3b`) - no blocking findings; terminology is consistent, limitations are safely stated, rejected alternatives/evidence are clear, and historical meeting-note warnings are acceptable as non-canonical historical material.
- Notes: Historical meeting notes and archived material were left unchanged because `current-state.md` is the present-tense source of truth and meeting notes are historical records.
