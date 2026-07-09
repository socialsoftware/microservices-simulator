# Scenario execution report v3 participants model

Date: 2026-07-08

Status: accepted

## Context

The current ScenarioExecutor report shape is single-saga-oriented: it stores one top-level saga instance id, saga FQN, input variant id, lifecycle outcome, and flat step outcomes. Multi-saga execution would make those fields ambiguous. Filling them with nulls for most multi-saga runs would make reports awkward, while choosing a primary or failed saga would misrepresent the scenario.

The report contract is still early and the verifier author is the only current module user, so this is the right point to make a clean breaking schema change before impact scoring, batch execution, search, or external consumers depend on the artifact.

## Decision

Introduce a canonical v3 scenario execution report model where scenario-level facts remain top-level and saga-specific facts live under a required participant list.

Top-level report fields should describe the execution attempt as a whole, including scenario execution id, scenario plan id, scenario kind, terminal status, assigned vector, vector source, provider mode, runtime metadata, fault slots, skipped-candidate counts, and scenario-level blockers.

Each participant describes one saga instance in the scenario and contains its saga instance id, saga FQN, input variant id, materialization/startup state, lifecycle outcome, step outcomes for that saga, skipped steps for that saga, and saga-local blockers or failure details.

Single-saga execution is represented as a v3 report with exactly one participant. Multi-saga execution is represented as the same report shape with multiple participants. The v3 model does not preserve top-level single-saga identity fields for compatibility with v2.

## Considered Options

### Canonical participants list for every report

Selected. This makes single-saga execution a degenerate case of multi-saga execution and gives future impact scoring one uniform artifact shape.

### Top-level nullable single-saga fields plus participants

Rejected. It avoids an immediate breaking change but creates null-heavy multi-saga reports and preserves the misleading idea that a scenario has one top-level saga identity.

### Participants plus single-saga compatibility summary

Rejected for the first v3 contract. It would ease migration, but it duplicates facts and encourages consumers to keep depending on a single-saga view. No compatibility bridge is needed because the module has no external users yet.

### Separate single-saga and multi-saga report types

Rejected. Separate schemas would keep each report type clean, but future impact scoring and search would need to normalize multiple artifact shapes. A unified participants model is simpler for downstream analysis.

### Top-level primary or failed saga summary

Rejected. A multi-saga scenario has no stable primary saga, and failure-based summaries become ambiguous when no saga fails or multiple saga instances fail.

## Consequences

- v3 report generation and tests may intentionally break v2 expectations.
- Existing single-saga report consumers in the repository should migrate to the participant list instead of top-level saga fields.
- The v3 model avoids null-heavy multi-saga reports and avoids misleading primary-saga semantics.
- Future impact scoring can iterate participants uniformly across single-saga and multi-saga attempts.
- Current implementation status remains v2 until the multi-saga executor/report feature is implemented and validated.
