# FaultScenario executable-content deduplication

An on-demand request must reuse a FaultScenario already persisted with the same exact
executable content, even when package projection makes a regenerated internal hash differ.
Executable equality is the current compact record without `id`: exact WorkloadPlan id,
fault vector, and the complete ordered `step`/`event`/`compensate` action references.

The WorkloadPlan identity continues to distinguish setup, participants, fault slots,
event origins and semantic recovery metadata. Action kind, reference and order remain
significant. Existing package IDs and records are retained; no historical package is
rewritten. If equivalent historical duplicates exist, new requests select the lowest ID
deterministically. Different executable content is never discarded.

No schema, global ID-generation, executor, ImpactV2, score-policy or Quizzes-specific
normalization change is in scope.
