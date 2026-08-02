# Harness Log - quizzes-full-2

Append-only. Schema and rules: `.claude/skills/_shared/conventions.md` § "Harness log".

| # | Session | Type | Artifact | Problem | Outcome | Ref |
|---|---------|------|----------|---------|---------|-----|
| 1 | 2.1.a | 2 | `.claude/skills/implement-aggregate/session-a.md` | Needed the sanctioned JPA realization of a P1 rule classified as "Java `final` field" (COURSE_NAME_FINAL, COURSE_TYPE_FINAL); the skill names the `non-final P1 rule` category but prescribes no field/constructor shape, so the no-arg-constructor and Hibernate-load-path handling was inferred. T1 never loads a row, so the inference is unproven until 2.1.b reads a Course back. | deferred | |
| 2 | 2.1.a | 2 | `.claude/skills/classify-and-plan/SKILL.md` | session-a.md requires `READ_{AGGREGATE}` in the saga-state enum when a later aggregate uses this one as a cross-aggregate prerequisite, but plan.md emits no per-aggregate saga-state set, so the slice had to scan later aggregates' plan.md sections and guess the enum; a mismatch would only surface in a later session's `c`. | deferred | |
