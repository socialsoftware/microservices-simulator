# Integration review

Status: passed for the approved copied-value slice; generated-case/runtime checks accompany
this review in HANDOFF and the retained evidence.

A separate implementation/review pass checked inference, observer lifecycle and evidence
joins. The parent inspected actual code and retained application reports.

Resolved findings:

- Fatal agent/contract mismatch now disables recording, rather than recording with stale
  contracts and relying only on a downstream fitness gap.
- Constructor evidence retains the exact admitted inbound transport-link event orders.
  The assessor joins the response path, input occurrence and these links, validates actor
  continuity, and requires constructor < registration < committed write.
- Full aggregate identity is used in joins. Registered writes without a matching commit
  produce no overwrite (transaction rollback); observer failures independently retain gaps.
- Late observer failures survive session closure. All application references are released
  at attempt end; unsupported cross-thread access yields a gap.
- Concrete destination aliases are individually checked against persisted paths, then
  grouped. They are not ambiguous merely because one object has two actual paths. A
  dedicated test proves two fields / one occurrence; duplicate collection keys remain gaps.
- The new policy is versioned; v1 retains its four weights. Missing or incomplete enabled
  evidence is unavailable, while disabled criteria do not block other complete components.

Scope findings retained for follow-up, not absorbed:

- Exact symbolic key evidence for UpdateTournament's Topic-ID collection is missing. The
  generated qualification explicitly uses the existing type-only fallback and proves the
  concrete relationship at runtime.
- General computed replacements and non-constructor writes need broader provenance.
- The performance sample includes process startup and is not a scaling result.

Final compile, shell syntax, diff whitespace and the 47-test Python suite pass. Three
ordinary generated replays with the new observer disabled exactly preserve the normalized
application observations and previous measurements, including existing coverage gaps.

No application business code, GA operators, RL or paper prose were changed by this package.
