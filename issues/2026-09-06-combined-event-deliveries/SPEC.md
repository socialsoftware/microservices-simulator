# Bounded combinations of event deliveries

## 1. What this is
Generated Saga/local experiments can combine different consumer routes of one persisted event, rather than observing only one selected delivery. The outcome is a reproducible comparison of isolated deliveries, subsets, orders and placements, with explicit execution boundaries.

## 2. Goals
1. Generate deterministic subsets and orders of supported routes of the same trigger/emission.
2. Execute selected deliveries with exact event identity and one eligible receiver per route.
3. Qualify a Quizzes deletion example and explain final effects in the Portuguese meeting note.

## 3. Non-goals
Multiple emissions combined into one experiment, nested event chains, retries, delivery faults, multiple receivers within one route, true parallel execution, scoring-policy changes, anomaly detection implementation or GA implementation.

## 4. Functional requirements
- FR-1: Retain base and singleton workloads. Add bounded route subsets, permutations and legal placements after their shared trigger, preserving outer forward order. Never combine unrelated trigger occurrences or emission sites. Enumerate deterministically under existing catalogue bounds without materializing an unbounded powerset.
- FR-2: Expose a maximum deliveries-per-workload configuration. Default 1 preserves existing generation behavior; qualification explicitly uses 3. A value of 1 reproduces the existing route/placement set and identities. Limits and truncation remain explicit.
- FR-3: Deliveries have distinct route/action identities, share the exact captured event and add no forward fault bits or compensation checkpoints. Trigger masking applies to every corresponding delivery.
- FR-4: Call real handlers synchronously, one unique eligible receiver per route. Missing/multiple receivers, handler failure, recursive emission or identity mismatch retain explicit hard stops, not successful zero scores. Eligibility is checked at each delivery; do not silently invoke a handler that is no longer eligible.
- FR-5: Setup must come from a supported coherent source fixture or existing explicit provider. Do not combine unrelated tests or invent receiver identities. Unsupported preparations remain non-executable. Existing descriptor-based route selection remains singleton-only; the qualified multi-route generation path uses coherent source-derived setup.
- FR-6: Assess the existing ImpactV2 categories after all selected actions/recovery. Preserve category evidence, writer identity, coverage and selected horizon; do not drain additional listeners.
- FR-7: Explain isolated/combined Quizzes deliveries and edge cases from actual reports. Document anomaly flags as complementary future evidence, separate from final-effect counts; do not claim anomaly detection is implemented.

## 5. Architecture
Preserve visitor/state/adapter/scenario/executor and simulator replay boundaries. Existing list-based event actions and current package roles remain the representation. Normal application polling remains unchanged outside replay.

## 6. Data model
One event occurrence can have several selected route-specific consequences. Every consequence appears once in the normal schedule. Existing logical receiver and writer identities remain authoritative.

## 7. Security model
Not affected. Existing method/route authorization and exact identity checks remain enforced.

## 8. Operating
Opt in to a small maximum (3 for qualification). Keep catalogue caps and fresh JVM/database attempts. Default 1 is the rollback configuration. No push, merge or production Quizzes repair.

## 9. Future roadmap
A bounded causal anomaly detector, then budgeted baseline/search evaluation. Broader asynchronous delivery semantics require a separate decision.

## 10. Open decisions
None blocking. User approved implementation of the bounded proposal; default 1 is the compatibility choice, not a restriction on the new explicit qualification.
