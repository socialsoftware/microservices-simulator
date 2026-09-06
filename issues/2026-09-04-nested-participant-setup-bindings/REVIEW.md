# Independent review

Date: 2026-09-05. Reviewer: Astra. Implementer: Sol Medium.

Disposition: accepted, with no blocking findings remaining. One formal final review,
preceded by incremental feedback on authoritative whole-result references, exact target
cutoff, duplicate occurrence semantics and parsed dummyapp coverage.

The review compared the source changes against the saved starting patch, preserving the
earlier unrelated dirty work. It inspected recursive participant mapping, source-reference
retention in Groovy recipes, adapter setup prefix construction, validator compatibility
and fixture changes. Root-result references terminate traversal; missing exact producers
cannot silently fall back to constructors; conflicting occurrence definitions invalidate
the plan. New setup targets are excluded from complete-fixture fallback even when their
target metadata is absent. Nested scalar property collections remain blocked by policy.

The additional dummyapp ItemDto validator entry and six-argument fixture constructor are
bounded fixture support, not new application dispatcher authority. A parsed source test
demonstrates reference retention through a real collection. Synthetic negative tests
exercise missing/later/selected-target references, conflicting occurrences, wrong types
and authoritative-root behavior. There is no standalone adapter mutation test removing
target metadata, nor a dedicated mapper test for the nested scalar-property exclusion;
those branches were reviewed directly. Quizzes integration checks exact target exclusion
for all 87 CreateQuestion setup occurrences and all three CreateQuiz occurrences.

Independent qualification:

- Recomputed 796 ordinary accepted singles as 664 source setup + 1 without setup + 131
  blocked. Exact baseline delta: 88 gained, zero lost; 85 CreateQuestion and 3 CreateQuiz.
- Compared all nine final/repeated package files byte for byte after Docker; identical.
- Executed four unchanged persisted scenarios in fresh Docker JVM/Spring contexts.
  Each returned SUCCESS / EXACT with successful setup and an empty event baseline.
  A qualification-only observer independently checked persisted course and nested
  question/topic IDs against setup results, and exactly one target creation.
- Audited the full test log: 734 tests in 47 suites, zero failures/errors/skips. Five stale
  XML reports total 53 additional tests and must not be counted. They have no current
  source files or entries in this run's log. This explains why directory totals are not
  comparable to the actual current run.

Evidence files: `verifiers/target/astra-nested-bindings/independent-counts.json`,
`independent-reproducibility.json`, `test-count-audit.json`, the four execution reports,
four `*-identity.json` sidecars and Docker logs.

No full 665-candidate runtime qualification is claimed. Impact semantics, scalar
property collections, larger-workload recounts and search remain outside this change.
