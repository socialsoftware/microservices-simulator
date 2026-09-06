# Implementation plan

## 1. Environment and execution mode
Documented route, reviewed execution in the primary checkout. User authorized implementing the preceding bounded proposal and updating roadmap/meeting material. Existing local linear commit authorization applies; no PR, merge or push. Preserve note-04-09-2026.md. No additional user-verdict pause is necessary before the working result.

## 2. Implementation strategy
Reuse list-based normal actions, route identity and exact event capture. Extend deterministic generation/configuration; validate existing replay rather than replacing it. Use independent bounded generation and runtime-proof work, followed by a separate review. Root owns integration, Quizzes execution and documentation.

## 3. Milestones
### M0 — Generated combinations (FR-1, FR-2, FR-3)
Add configuration and lazy bounded enumeration grouped by exact trigger/emission. Verify 3 routes produce 15 nonempty ordered selections when there are no later outer steps, placement causality, caps, shuffled-input determinism and singleton compatibility. Dummyapp-first tests and current package roundtrip.
### M1 — Replay proof and real application (FR-3 through FR-6)
Verify successive distinct routes share one captured event; masking covers all; missing/multiple receivers and handler failures remain explicit. Inspect setup for the CourseExecution deletion fixture. Generate a fresh package and execute base, isolated routes and both orders against fresh state, plus trigger-fault masking. Use Docker for Quizzes runtime evidence and retain every attempt.
### M2 — Interpretation and review (FR-7)
Update current-state, roadmap, event ADR and Portuguese meeting note. Record concrete outcomes and limits; place complementary anomaly work before/alongside the later search evaluation without implementing it. Independent review of generation/runtime boundaries and proof, then local commit.

## 4. Validation strategy
Focused Spock and simulator tests as changed; full verifier regression after integration. Docker prepared build and small selected Quizzes campaign. Validate report/package identities, action order, exact shared event, scores and retained invalid attempts. Build documentation and check links/diff.

## 5. Risks and fallbacks
Combination growth: lazy enumeration plus explicit catalogue and delivery caps. Setup ambiguity: retain unsupported status, use a coherent source fixture. Earlier delivery changing later eligibility: preserve fail-closed checks and report this boundary. New source/contract discoveries outside one-event multi-route scope require user discussion. Runtime cost is limited to the discriminating campaign, not another full catalogue run.
