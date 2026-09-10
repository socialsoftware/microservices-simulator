# Harness Retrospective — quizzes-full-2

**App:** quizzes-full-2
**Date:** 2026-08-11
**Harness-log rows:** 89 (Type 1 48 / Type 2 38 / 2-fw 2, plus one compound `1+2` row)
**Sessions with friction:** 18 of 29 session ids in plan.md (plus 3 aggregate-boundary pseudo-sessions and `review`)
**Harness commits:** 75
**Verdict:** Harness did not converge

> **Harness converged** = repairs decline toward zero across the run, no Step 7 **Ratified a guess**
>   verdict, no open `2-fw` row, build green.
> **Harness converged with gaps** = repairs decline but confirmed gaps remain open, and no session
>   was blocked by one.
> **Harness did not converge** = a flat or rising repair rate, or any **Ratified a guess** or
>   **Fix wrong** verdict, or a red build attributable to the harness.

---

## Run Summary

| | |
|---|---|
| Sessions executed | 29 (60 plan.md checkboxes, all ticked) |
| Harness-log rows | 89 (T1 48 / T2 38 / 2-fw 2 / compound `1+2` 1) |
| Outcomes | fixed 70 / declined 0 / deferred (open) 6 / deferred (closed) 13 |
| `harness:` commits on the run | 75 |
| Harness edits without a `harness:` prefix | 0 |
| `Ref`s made stale by a history rewrite | 7 rows (6 distinct commits) |
| Type 2 halts | 16 halted / 13 proceeded (12 further rows are closers, not halts) |
| Build outcome | MAVEN_EXIT=0, tests=338 failures=0 errors=0 skipped=0 |

The run ends green and with no wrong harness edit: across 89 rows there is **no `Fix wrong` verdict and
no `Ratified a guess` verdict**. That is the strongest thing this run says about the unilateral Type 1
gate — it was not used to ratify an agent's own guess even once, and the one candidate proposed by an
evidence batch (row 87) did not survive a direct check. What the run does not show is convergence.
Session-driven rows per aggregate run 10, 8, 6, 13, 9, 2, 4, 9; the curve dips at aggregates 6-7 and
returns to 9 at aggregate 8, and **all six still-open gaps sit in that final aggregate**. The harness
cleared its factual errors and never closed its silences: every open row is Type 2, and five of the
six were resolved for the application by a human decision that was deliberately not generalised into
`docs/` or `.claude/`. A ninth aggregate would meet the same silences.

The second finding is about the gate rather than the harness text. **Eight Type 1 rows are
misclassified**, and six of them share one origin: a `/review-artifacts` **Check 3 — Improvement
Opportunities → Missing Examples** table, which is by construction a list of silences, not
contradictions. Those were fixed unilaterally under a fast path that `AGENTS.md` reserves for
mechanically demonstrable contradictions. No harm reached the text — each edit restates a rule already
owned elsewhere — so this is a process finding, not a correctness one.

Per-type counts sum to 90 against a row total of 89, because row 50 carries the compound `Type` value
`1+2` and is counted once in the row total and once in each of the Type 1 and Type 2 buckets.

Harness-log integrity: numbering is clean and gapless, 1-89. Findings:
- **Row 50 uses a compound `Type` value (`1+2`)**, which `_shared/conventions.md` § "Harness log"
  does not admit; it names `1`, `2` and `2-fw` only.
- **Row 69 is `fixed` with an empty `Ref`.** Its change is recoverable only through
  `git log -S` against the application commit `cef73bb65`.
- **Two rows name artifacts outside the harness** and are misfiled per Step 3.a: row 69 names
  `2026-08-08-tournament-2.8.c-plan.md`, an untracked file under `tmp/` excluded by
  `.git/info/exclude`; row 71 names `quizzes-full-2-aggregate-grouping.md`, an application spec.
  Row 55's `Artifact` cell additionally names "the seven `{Aggregate}Repository.java` of this
  application" alongside three legitimate harness paths.
- **Seven rows carry a `Ref` invalidated by a history rewrite** — 31, 32, 33, 34, 35, 62, 63. Each
  sha resolves but fails `git merge-base --is-ancestor`. All six distinct commits were recovered
  on-branch by identical subject and judged on the replacement: 31→`bd2d61973`, 32→`d12d1170d`,
  33→`739dd2f64`, 34→`397146a13`, 35→`0581cb8a0`, 62 and 63→`e9611b673`. The session retros carry the
  same stale shas, consistent with a rewrite after both were written.
- **Twelve `harness:` commits carry no harness-log row**: `53ebf1fb7`, `9c72033d0`, `f65c705da`,
  `94daddd93`, `fe30bd0e1`, `52e9579eb`, `8c83170e2`, `0814cf8ac`, `44b785c8c`, `76ab595e6`,
  `c743e9e5e`, `998910d32`. The largest is `76ab595e6`, whose own subject is "close the thirteen
  Minor rows from review-2026-08-08" — thirteen findings repaired with no row between them.
- **Zero harness-path commits lack the `harness:` prefix**, so the commit convention itself held.
- The append-only log was edited in place three times to backfill `Ref` values (`bde28c668`,
  `2b9cfc019`, `24fd73bac`, and inside `76ab595e6` and `303a5978`). Backfilling an empty `Ref` is
  consistent with the schema's intent, but two of those edits ride inside commits that also carry
  harness text changes rather than standing alone.

---

## Rows by Artifact

| Artifact | Rows | T1 | T2 | 2-fw | fixed | declined | deferred (open) | deferred (closed) | What the friction was about |
|----------|------|----|----|------|-------|----------|------------------|--------------------|-----------------------------|
| `.claude/skills/implement-aggregate/session-c.md` | 17 | 10 | 8 | 0 | 15 | 0 | 0 | 2 | T4/compensation-test scope, the SpockTest fixture-helper swap, event-test scoping |
| `.claude/skills/implement-aggregate/session-b.md` | 12 | 6 | 6 | 0 | 10 | 0 | 0 | 2 | Fixture helpers: foreign ids, sibling helpers, state a create cannot reach |
| `docs/concepts/testing.md` | 12 | 3 | 9 | 0 | 9 | 0 | 1 | 2 | T2 read-back, T4 happy path, compensation read-back, time-gated state |
| `.claude/skills/classify-and-plan/SKILL.md` | 11 | 7 | 4 | 0 | 10 | 0 | 0 | 1 | Command paths, saga-state and sentinel sets, `{Query}` doubling, P4 proxies |
| `.claude/skills/implement-aggregate/session-a.md` | 9 | 5 | 4 | 0 | 7 | 0 | 0 | 2 | JPA mapping prose, collection helpers, sentinel placement, repository typing |
| `docs/concepts/events.md` | 9 | 5 | 4 | 0 | 8 | 0 | 0 | 1 | `@Entity`, ByEvent naming, `subscribesEvent()`, version backlog, cascade step 3 |
| `.claude/skills/implement-aggregate/session-d.md` | 8 | 4 | 4 | 0 | 7 | 0 | 0 | 1 | ByEvent shape, re-affirming payloads, shared-file ownership |
| `docs/concepts/rule-enforcement-patterns.md` | 7 | 3 | 4 | 0 | 5 | 0 | 2 | 0 | P2 recipe, Decision Guide Step 4 and the silences it does not cover |
| `docs/concepts/commands.md` | 6 | 4 | 2 | 0 | 5 | 0 | 0 | 1 | Jackson round-trip, missing symbols, `rootAggregateId` null cases |
| `docs/concepts/sagas.md` | 4 | 1 | 3 | 0 | 2 | 0 | 2 | 0 | `× N` data assembly, `SagaCommand`, create-saga field provenance |
| `docs/concepts/service.md` | 3 | 1 | 2 | 0 | 2 | 0 | 0 | 1 | Constructor injection, sub-collection criterion |
| `docs/architecture.md` | 3 | 3 | 0 | 0 | 3 | 0 | 0 | 0 | Package layout, per-event handler node, R8 example |
| `docs/concepts/aggregate.md` | 3 | 3 | 0 | 0 | 3 | 0 | 0 | 0 | `version` type, repository typing, custom-repository delegation |
| `.claude/skills/implement-aggregate-full/SKILL.md` | 3 | 3 | 0 | 0 | 3 | 0 | 0 | 0 | Shared files per session type, slice-brief `-Dtest` referent |
| `.claude/skills/_shared/conventions.md` | 2 | 2 | 0 | 0 | 2 | 0 | 0 | 0 | Stale surefire totals, truncated `MAVEN_EXIT` |
| `docs/workflow.md` | 2 | 2 | 0 | 0 | 2 | 0 | 0 | 0 | Package-layout trees |
| `.claude/skills/review-artifacts/SKILL.md` | 2 | 0 | 2 | 0 | 2 | 0 | 0 | 0 | Scaffold templates and `AGENTS.md` outside the review set |
| `.claude/skills/implement-aggregate/SKILL.md` | 2 | 2 | 0 | 0 | 2 | 0 | 0 | 0 | Unused `{appClass}`, step ordering |
| `simulator/.../ms/aggregate/EventHandler.java` | 2 | 0 | 0 | 2 | 2 | 0 | 0 | 0 | Polymorphic `findAll()`, added then reverted |
| `.claude/skills/boot-strap/templates/AppSpockTest.groovy.template` | 1 | 0 | 1 | 0 | 1 | 0 | 0 | 0 | `flushAndClear()` helper |
| `.claude/skills/harness-retrospective/SKILL.md` | 1 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | Pathspec and `grep`-derived verdict |
| `.claude/skills/boot-strap/SKILL.md` | 1 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | `rg`-derived clean verdict |
| `AGENTS.md` | 1 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | Module map named a directory that never existed |
| `2026-08-08-tournament-2.8.c-plan.md` (misfiled) | 1 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | Selection predicate vs plan.md P4b |
| `quizzes-full-2-aggregate-grouping.md` (misfiled) | 1 | 0 | 1 | 0 | 0 | 0 | 1 | 0 | Event anchor circularity in the app spec |

Counts are by first-named artifact for the row totals and by every named artifact for the type
columns, so the columns exceed the row count where a row names several files.

**`.claude/skills/implement-aggregate/session-c.md` is the harness's largest single weakness for this
run**, with 17 rows. But it is a weakness that healed: all 15 of its `fixed` rows hold today and both
its deferred rows were closed, leaving nothing open. The artifacts still owed a lesson are
`docs/concepts/rule-enforcement-patterns.md` and `docs/concepts/sagas.md` — two open rows each, and
between them four of the run's six open gaps.

## Rows by Type

| Type | Rows | Notes |
|------|------|-------|
| 1 | 48 | Cleared unilaterally. 40 Sound, 1 over-broad, 8 misclassified, 0 ratified a guess. |
| 2 | 38 | The harness's silences. Every open row at the end of the run is one of these. |
| 2-fw | 2 | Rows 54 and 55, one add/revert pair on `EventHandler.java`; net framework delta zero. |
| 1+2 | 1 | Row 50, a compound value the schema does not admit; audited in both buckets. |

---

## Convergence

| Aggregate / phase | Sessions | Type 1 | Type 2 | 2-fw | Total |
|-------------------|----------|--------|--------|------|-------|
| 1 — Course | 2.1.a, 2.1.b, 2.1.c | 5 | 5 | 0 | 10 |
| boundary 1→2 | 2.1/2.2 | 0 | 4 | 0 | 4 |
| 2 — User | 2.2.c | 5 | 3 | 0 | 8 |
| 3 — Topic | 2.3.b, 2.3.c | 0 | 6 | 0 | 6 |
| 4 — Execution | 2.4.a-d, 2.4 | 5 | 8 | 0 | 13 |
| 5 — Question | 2.5.c, 2.5 | 6 | 4 | 0 | 9 |
| 6 — Quiz | 2.6.a, 2.6.c | 2 | 0 | 0 | 2 |
| 7 — QuizAnswer | 2.7.c, 2.7.d | 0 | 2 | 2 | 4 |
| 8 — Tournament | 2.8.b, 2.8.c, 2.8.d | 3 | 6 | 0 | 9 |
| (non-session) | review | 23 | 1 | 0 | 24 |

**Pattern:** flat, with a late rise — the run is long enough for the distinction to mean something
(eight aggregates, 65 session-driven rows), so this is a trend and not a small-sample artefact.

Session-driven totals run 10, 8, 6, 13, 9, 2, 4, 9. There is a real decline from aggregate 4's peak of
13 to aggregate 6's 2, and had the run stopped there it would read as convergence. It does not hold:
aggregate 8 returns to 9 rows, the joint-second-highest of the run, and none of the count ever reaches
and stays at zero.

Read by type the picture is sharper, and it is the specific result § Step 6 asks about. **Type 1
declines and Type 2 does not.** Type 1 per aggregate: 5, 5, 0, 5, 6, 2, 0, 3 — noisy but trending
down, and the aggregate-8 residue is three rows of which two are first-use JPA and `SagaCommand`
snippet errors that nothing had exercised before. Type 2 per aggregate: 5, 3, 6, 8, 4, 0, 2, 6 — no
trend at all, and it *ends* near its peak. That is the expected shape: a factual error is cleared once
by whoever trips over it, while a silence is only ever filled by a human decision, and this run made
those decisions per occurrence rather than generalising them.

The artifacts that recur across aggregates are the structural ones:
`.claude/skills/implement-aggregate/session-b.md` and `session-c.md` (aggregates 1, 2, 3, 4, 6),
`docs/concepts/testing.md` (1, 2, 3, 5, 8), `docs/concepts/rule-enforcement-patterns.md` (7, 8) and
`docs/concepts/sagas.md` (5, 8). The first three absorbed their lessons — they carry no open rows. The
last two did not, and they are where the standing gaps live.

One confound worth naming: 24 of the 89 rows carry Session `review` rather than a session id, and
23 of those are Type 1. They cluster on 2026-08-08 and 2026-08-10, after aggregate 8's code was
written. They measure how much latent inconsistency the static `/review-artifacts` sweeps found once
someone went looking, not friction an implementing session hit — so they are excluded from the
convergence curve above and reported separately.

---

## Fixes Re-Checked

70 rows carry `Outcome = fixed`. **68 hold today.** The two that do not are listed first.

| # | Artifact | Ref | Verdict | Evidence |
|---|----------|-----|---------|----------|
| 35 | `.claude/skills/_shared/conventions.md` | `0581cb8a0` (logged `42ba0086`, stale) | **Fix incomplete** | § "Run the test suite" now says at :162-165 "**Redirect to a file** - `> build.log 2>&1` is a redirection, not a pipe", but neither command block it governs was changed: :167-173 still shows `mvn clean -Ptest-sagas test` / `echo "MAVEN_EXIT=$?"` and :181-185 the narrow form, both without a redirect. The block an agent copies still does not do what the prose mandates. |
| 54 | `simulator/.../EventHandler.java` | `73d29e458` | **Fix reverted or superseded** | Removed wholesale by `a13c54869` (row 55). Current `getAggregateIds()` carries no filter and no `aggregateType()`; a whole-tree scan for `aggregateType()` returns zero hits. Deliberate supersession, not decay. |
| 3 | `classify-and-plan/SKILL.md` | `700600ac` | Fix holds | All five `commands/{aggregate}` occurrences are now `{src}`-prefixed; zero unrooted. |
| 4 | `docs/concepts/commands.md` | `90e5fe00` | Fix holds | `protected AddShipmentItemCommand() {}` plus setters at :16, :28-29, with the mandatory-not-stylistic callout at :33-36. |
| 5 | `docs/concepts/commands.md` | `90e5fe00` | Fix holds | `private static final Logger logger = ...` at :166 and the explanatory line at :197-198. |
| 6 | `session-b.md` | `90e5fe00` | Fix holds | The `@Autowired(required = false) protected AggregateIdGeneratorService` block at :220-227. |
| 8 | `session-c.md` | `688cb5f7` | Fix holds | § Produce :50-54 blockquote "One required edit lives outside this section." |
| 11 | `classify-and-plan/SKILL.md` | `9a1b1b4c` | Fix holds | § 6.d computes the set; no `READ_{AGGREGATE}` token remains in the file. |
| 12 | `testing.md`, `AppSpockTest.groovy.template` | `0843bdef` | Fix holds | testing.md:198-199 "Every read-back calls `flushAndClear()` first"; the template ships the helper. |
| 13 | `session-b.md`, `session-c.md` | `ea7202e7` | Fix holds | session-b.md:114-116 prescribes the string literal; session-c.md's stack-trace derivation is gone. |
| 14 | `service.md`, `session-c.md` | `303a5978` | Fix holds | service.md:35-44 "A service declares no `@Autowired` field"; session-c.md:289-298 requires both edits. |
| 15 | `testing.md` | `16b441f6` | Fix holds | :440-450 "**`commandGateway` is not inherited.**" — verified: neither SpockTest declares it, and BeanConfigurationSagas registers two gateways. |
| 16 | `session-c.md` | `40e3414b` | Fix holds | § "One `{Op}CompensationTest.groovy` ..." :224, :242-258, with the applicability test. |
| 17 | `session-c.md` | `c49f95e4` | Fix holds | :226-240 names both files and the CSV selection rule. |
| 18 | `conventions.md` | `ca2ebbee` | Fix holds | :192-197 the stale-report caveat on the no-clean narrow form. |
| 19 | `testing.md`, `session-c.md` | `6ee9a985` | Fix holds | testing.md:426-438 the unresolvable-aggregate exception; session-c.md carries the pointer. |
| 21 | `classify-and-plan/SKILL.md` + 3 | `e54b1353` | Fix holds | § 6.e computes the sentinel set; Step 7 2.N.a row and Step 8 template both carry it. |
| 22 | `workflow.md`, `architecture.md`, `session-a.md` | `8aac2ccd` | Fix holds | Both layout trees admit `domain/`; session-a.md:242-245 carries the halt clause. |
| 26 | `commands.md` | `c35c4a88` | Fix holds | :63-67 the bulk-read blockquote, forbidding the filter's foreign id. |
| 27 | `session-b.md` | `886d531a` | Fix holds | :241-251 foreign-id params "required and undefaulted, and come first". |
| 28 | `testing.md` | `8a0da2d3` | Fix holds | :505-511 "uses whatever read coordinator the aggregate actually exposes" plus the never-add prohibition. |
| 29 | `session-a.md` | `fda4dd45` | Fix holds | :59 "with **no** `mappedBy` — the aggregate is the owning side". One-line diff. |
| 30 | `session-a.md` | `9dde2a4c` | Fix holds | :65 add/remove helpers "are setters, not business logic". |
| 31 | `session-b.md` | `bd2d61973` (logged `a85cd8f7`, stale) | Fix holds | § "Fixture state a create cannot reach" :262-273, as amended by rows 32 and 33. |
| 32 | `session-b.md` | `d12d1170d` (logged `8c019542`, stale) | Fix holds | :274-282 loads via `aggregateLoadAndRegisterRead` plus the copy constructor. |
| 33 | `session-b.md` | `739dd2f64` (logged `3271780e`, stale) | Fix holds | :288-297 the upstream-minted foreign id. See Step 7 — the diff is over-broad. |
| 34 | `session-c.md` | `397146a13` (logged `a153d946`, stale) | Fix holds | :163-168 "class-scoped and added once". |
| 39 | `session-c.md` | `2cd9e00d8` | Fix holds | :173-181 one negative case per test class, with the per-type fallback. |
| 40 | `session-d.md`, `testing.md` | `f214d2994` | Fix holds | session-d.md:191-206 the two-step rule; testing.md:59 the § Fake carve-out. |
| 41 | `events.md`, `session-d.md` | `cd0a2bd08` | Fix holds | events.md:79-87 "This backlog is expected behaviour"; :286 "Always advance the cached publisher version." |
| 42 | `sagas.md` | `62c3cf6bb` | Fix holds | § "Collection-valued data-assembly step" :402-431, including the literal-step-name rationale. |
| 43 | `aggregate.md`, `events.md`, `session-d.md` | `9bd18a767` | Fix holds | aggregate.md:13 `Long`; events.md:284 "Every cached publisher version is a `Long`." Verified `Aggregate.java:30` is `private Long version;` and no `VersionService` class exists. |
| 44 | `events.md` | `f4bfc19cd` | Fix holds | `@Entity` on both the `CreateShipmentEvent` example and the canonical snippet. |
| 45 | `workflow.md`, `testing.md` | `ffb7d1970` | Fix holds | Both trees place `SpockTest.groovy` in the parent package, matching boot-strap and the app. |
| 46 | `commands.md`, `session-d.md` | `331796339` | Fix holds | :180-181 `cmd.getShipmentAggregateId()`; session-d.md:18 `getSubscribedAggregateId()`. |
| 47 | `implement-aggregate-full/SKILL.md` | `6ecc1528b` | Fix holds | § "Shared files per session type" `d` row lists all three appended files; ownership is per helper body. |
| 48 | `classify-and-plan/SKILL.md` | `cd81011ba` | Fix holds | Step 7 2.N.c row lists the pair with the gate note deferring to session `c`. |
| 49 | `review-artifacts/SKILL.md` | `4cddc5020` | Fix holds | Step 1.b `find` includes `*.template`; § 3.b table added. |
| 50 | `testing.md` + 3 | `ec99396dc` | Fix holds | All four Type 1 halves present; `loadForCheck` documented at testing.md:316-328. |
| 51 | `session-a.md` | `817fb30fc` | Fix holds | :60 `mappedBy` "names the back-reference field **on the entity**"; :77 the entity side. |
| 52 | `session-c.md` | `2457b5bbe` | Fix holds | :340-358 the value-repair clause and the read-test consequence. |
| 55 | `EventHandler.java` + 4 | `a13c54869` | Fix holds | Verified: `git diff master HEAD -- EventHandler.java` is 0 bytes, and all eight app repositories extend `JpaRepository<{Concrete}, Integer>`. |
| 56 | `rule-enforcement-patterns.md` + 2 | `475e157ef` | Fix holds | § Decision Guide Step 4 :105-150, with pointers live in testing.md:128-131 and session-c.md:204. |
| 57 | `aggregate.md` | `5ae093665` | Fix holds | :95 and :99-105 the concrete repository typing and its rationale. |
| 58 | `rule-enforcement-patterns.md` | `5ae093665` | Fix holds | § P2 :261-264 is a pointer; the four-step recipe is gone. `handleExternalChange` survives only in the review report that named it. |
| 59 | `session-b.md` | `5ae093665` | Fix holds | :60 "the service does **not** fetch it ... violates R2". |
| 60 | `classify-and-plan/SKILL.md` | `546a966d6` | Fix holds | § 3.d justification rewritten around the reference-object shape; no `subscribesEvent` claim remains. |
| 61 | `events.md` | `546a966d6` | Fix holds | :246 `<operation>ByEvent(...)`; :251-252 "The `ByEvent` suffix is **mandatory**". |
| 62 | `architecture.md` | `e9611b673` (logged `4b898dbff`, stale) | Fix holds | :87-93 "single dispatcher - one per aggregate, not per event". |
| 63 | `classify-and-plan/SKILL.md` | `e9611b673` (logged `4b898dbff`, stale) | Fix holds | The "typically" hedge is gone; the blockquote defers to session `c`. |
| 69 | plan.md (via `cef73bb65`) | *(empty)* | Fix holds | `plan.md:74` records "The selection predicate is the rule's own containment, `topics(question) ⊆ Tournament.topics`". |
| 70 | `sagas.md` | `a17ec02fd` | Fix holds | :91-108 wraps in `SagaCommand`, with the generics-invariance trap stated once. Verified `setForbiddenStates` is declared on `SagaCommand:29` and absent from `Command`. |
| 72 | `events.md`, `session-d.md` | `2b2b67ad3` | Fix holds | :73 the corrected mechanism. Verified `EventService.getSubscribedEvents` ends `.filter(eventSubscription::subscribesEvent).toList()`. |
| 73 | `review-artifacts/SKILL.md` | `bdc39dbb0` | Fix holds | `ls AGENTS.md` in Step 1.b; `TREES` list replaces the `PATHSPEC[:3]` slice. |
| 74 | `harness-retrospective/SKILL.md` | `a3cea15dc` | Fix holds | Step 2 uses `python3` and reports lines scanned; the widened scope survives as this skill's `TREES`. |
| 75 | `boot-strap/SKILL.md` | `7b8afb6e8` | Fix holds | Step 6.1 is a `python3` walk reporting files scanned. |
| 76 | `rule-enforcement-patterns.md`, `aggregate.md` | `02a527d8a` | Fix holds | § P2 :240-259 the consumer-side fragment; aggregate.md:130-140 the worked delegation. |
| 77 | `classify-and-plan/SKILL.md` | `bad7b7b8e` | Fix holds | Zero occurrences of `Get{Query}` remain in the file. |
| 78 | `classify-and-plan/SKILL.md` | `727f359d3` | Fix holds | Step 7 2.N.d row lists the three appended files. |
| 79 | `AGENTS.md` | `05cfa8263` | Fix holds | Module map names `logs/` and drops `reviews/`; verified no `reviews/` under the app. |
| 80 | `implement-aggregate-full/SKILL.md` | `ba6f1b437` | Fix holds | "this slice's own test class(es)" plus the per-session-type table. |
| 81 | `implement-aggregate/SKILL.md` | `c9531411d` | Fix holds | Harness-log step renumbered `6b`, above the retro row. |
| 82 | `service.md` | `22548c52b` | Fix holds | :181 "which buy nothing when the sub-collection is not the operation's primary intent". |
| 83 | `classify-and-plan/SKILL.md` | `43960fe5f` | Fix holds | Step 8 template reads `- {Query}({args})`. |
| 84 | `rule-enforcement-patterns.md` | `e8b3e0637` | Fix holds | :113-123 case 1 carries the `// Fetching this DTO enforces the precondition implicitly.` block inline. |
| 85 | `architecture.md` | `112f3eec1` | Fix holds | :241-255 the WRONG/RIGHT pair. |
| 86 | `commands.md` | `2c87432c7` | Fix holds | :54-61 the create-command constructor showing `super(unitOfWork, serviceName, null)`. |
| 87 | `events.md` | `176df6262` | Fix holds | § Cascade Invalidation step 3 :341-362 carries the subscription, the ByEvent method and the branch criterion. |
| 88 | `classify-and-plan/SKILL.md` | `8baa57850` | Fix holds | Step 4 gives both P4 branches a stated proxy, with the Decision Guide winning any disagreement. |
| 89 | `implement-aggregate-full/SKILL.md` | `eac1d0e5f` | Fix holds | The `b` row carries the omit-when conditional. |

---

## Post-Hoc Justification of the Type 1 Edits

49 rows name Type 1 — the 48 pure Type 1 rows plus row 50's Type 1 half. Every one was made
unilaterally, mid-session, with no human gate. Rows accepted on a batch quote are marked; the rest I
read myself.

| # | Session | Artifact | Claimed contradiction | Demonstrable? | Verdict |
|---|---------|----------|----------------------|---------------|---------|
| 3 | 2.1.b | `classify-and-plan/SKILL.md` | Command package contradicts `commands.md` § File Location | Yes — paths do not resolve to the app source root | Sound |
| 4 | 2.1.b | `commands.md` | Example fails Jackson round-trip under `serialize: true` | Yes — profile setting and `Command`'s own no-arg ctor | Sound |
| 5 | 2.1.b | `commands.md` | `logger` undeclared on `CommandHandler` | Yes — missing symbol | Sound |
| 6 | 2.1.b | `session-b.md` | `aggregateIdGeneratorService` undeclared in the template | Yes — missing symbol | Sound |
| 8 | 2.1.c | `session-c.md` | § Produce declares itself the manifest but omits a mandatory edit | Yes — self-contradiction in one file | Sound |
| 15 | 2.2.c | `testing.md` | `commandGateway` not inherited; interface injection ambiguous | Yes — missing symbol, two beans | Sound |
| 16 | 2.2.c | `session-c.md` | § Produce omits a file `testing.md` makes mandatory | Yes — two files disagree | Sound |
| 17 | 2.2.c | `session-c.md` | CSV omitted from the produced-files list | Yes — transcribes `testing.md`'s own rule | Sound |
| 18 | 2.2.c | `conventions.md` | Narrow form's totals include stale reports | Yes — mechanical | Sound |
| 22 | 2.2.c | `workflow.md`, `architecture.md`, `session-a.md` | Layout trees exclude a path `session-a.md` mandates | Yes — two files disagree | Sound |
| 29 | 2.4.a | `session-a.md` | `@OneToMany` prose names the wrong owning side | Yes — persistence-unit init fails | Sound |
| 32 | 2.4.b | `session-b.md` | Template calls a service method that returns a DTO | Yes — no such route in the same file's mandated surface | Sound |
| 33 | 2.4.c | `session-b.md` | Sibling-helper call sites throw on the 2.N.c swap | Yes — six observed call sites | **Sound but over-broad** |
| 34 | 2.4.c | `session-c.md` | Class-scoped `EventService` field duplicated across slices | Yes — compile error | Sound |
| 35 | 2.4.c | `conventions.md` | Truncated stdout swallows `MAVEN_EXIT` | Yes — observed | Sound |
| 43 | 2.5 | `aggregate.md`, `events.md`, `session-d.md` | `version` typed `Integer`; `VersionService` does not exist | Yes — `Aggregate.java:30` is `Long` | Sound |
| 44 | 2.5 | `events.md` | Prose mandates `@Entity`; examples omit it | Yes — self-contradiction | Sound |
| 45 | 2.5 | `workflow.md`, `testing.md` | Trees describe a layout no app has | Yes — boot-strap and the app disagree | Sound |
| 46 | 2.5 | `commands.md`, `session-d.md` | `getAggregateId()` on `Command` / `EventSubscription` | Yes — missing symbols | Sound |
| 47 | 2.5 | `implement-aggregate-full/SKILL.md` | Shared-file list omits three files session-d.md requires | Yes — two skills disagree | Sound |
| 50 | 2.5 | `testing.md` + 3 | Four defects incl. a one-arg `registerChanged` | Yes — signature is two-arg | Sound |
| 51 | 2.6.a | `session-a.md` | `mappedBy` names the aggregate's own field | Yes — EMF init fails | Sound |
| 52 | 2.6.c | `session-c.md` | Two instructions unsatisfiable together for a clock-stamped field | Yes — 14 observed errors | Sound |
| 57 | review | `aggregate.md` | Prescribes the typing row 55 reverted away from | Yes — two files disagree | Sound |
| 58 | review | `rule-enforcement-patterns.md` | P2 recipe contradicts `events.md` three ways | Yes — `handleExternalChange` exists nowhere | Sound |
| 59 | review | `session-b.md` | Read bullet requires a foreign `*Service`, forbidden by R2 | Yes — contradicts the same file 33 lines later | Sound |
| 60 | review | `classify-and-plan/SKILL.md` | Justification rests on an override `events.md` forbids | Yes — two files disagree | Sound |
| 61 | review | `events.md` | Canonical snippet elides the mandatory `ByEvent` suffix | Yes — self-contradiction | Sound |
| 62 | review | `architecture.md` | Per-event handler node contradicts three artifacts | Yes | Sound |
| 63 | review | `classify-and-plan/SKILL.md` | Hedge contradicts the same blockquote's own deferral | Yes — self-contradiction | Sound |
| 69 | 2.8.c | *(session working doc)* | D1's overlap predicate violates plan.md's P4b containment | Yes — mechanical against plan.md | Sound |
| 70 | 2.8.c | `sagas.md` | `setForbiddenStates` called on `Command` | Yes — declared on `SagaCommand` only | Sound |
| 72 | 2.8.d | `events.md`, `session-d.md` | "has no effect" is false; `EventService` does call it | Yes — verified in `simulator/` | Sound |
| 74 | review | `harness-retrospective/SKILL.md` | Pathspec too narrow; `grep`-derived verdict | Yes — contradicts `conventions.md` | Sound |
| 75 | review | `boot-strap/SKILL.md` | `rg`-derived clean verdict | Yes — contradicts `conventions.md` | Sound |
| 76 | review | `rule-enforcement-patterns.md`, `aggregate.md` | Missing consumer-side example / delegation body | **No** — review Check 3 "Missing Examples" | **Misclassified** |
| 77 | review | `classify-and-plan/SKILL.md` | `{Query}` doubling yields `GetGetOpenShipmentsCommand` | Yes — the Step's own substitution rule | Sound |
| 78 | review | `classify-and-plan/SKILL.md` | 2.N.d row omits three appended files | **No** — the row concedes it was "legal rather than wrong" under plan.md's blueprint clause | **Misclassified** |
| 79 | review | `AGENTS.md` | Module map names a directory that never existed | Yes — verified absent | Sound |
| 80 | review | `implement-aggregate-full/SKILL.md` | `-Dtest` placeholder has no referent for `a`/`d` | Yes — two skills disagree | Sound |
| 81 | review | `implement-aggregate/SKILL.md` | Step order contradicts its own preamble and `session-completion.md` | Yes | Sound |
| 82 | review | `service.md` | "disproportionate" names no threshold | **No** — review graded it "Minor / Decorative wording" | **Misclassified** |
| 83 | review | `classify-and-plan/SKILL.md` | Second site of row 77's doubling | Yes | Sound |
| 84 | review | `rule-enforcement-patterns.md` | Case 1 lacks the code block case 2 has | **No** — review Check 3 "Missing Examples" | **Misclassified** |
| 85 | review | `architecture.md` | R8 prose-only where R7 has a pair | **No** — review Check 3 "Missing Examples" | **Misclassified** |
| 86 | review | `commands.md` | Which `super(...)` argument goes `null` is left to inference | **No** — review Check 3 "Missing Examples" | **Misclassified** |
| 87 | review | `events.md` | Cascade step 3 is prose with no branch criterion | **No** — review Check 3 "Missing Examples" | **Misclassified** |
| 88 | review | `classify-and-plan/SKILL.md` | P4a/P4b predicates defined nowhere | Gap yes, but the fix was a human design choice | **Misclassified** |

**Verdict ratio:** Sound 40 / Sound but over-broad 1 / Ratified a guess 0 / Misclassified 8

**The unilateral gate was safe on this run, and the evidence for that is stronger than the raw count
suggests.** Not one of the 49 edits ratified a guess. One evidence batch proposed *Ratified a guess*
for row 87, and I overruled it after reading `176df6262^`: `session-d.md` already carried the branch
criterion ("can this consumer aggregate still fulfil its purpose if the referenced entity is gone?"),
so the edit transcribed an existing rule one level down rather than inventing an answer. The same test
clears rows 76, 82, 84, 85 and 86 — in each, the added text restates a rule already owned elsewhere
and carries an explicit ownership pointer.

What the ratio does show is a **systematic mis-bucketing rather than eight isolated slips**. Six of
the eight misclassified rows (76, 82, 84, 85, 86, 87) originate in a `/review-artifacts` **Check 3 —
Improvement Opportunities** table, whose own column heading is "Why an example would help". Check 3 is
by construction a list of silences; Check 2 is the contradiction check. A finding filed under Check 3
is Type 2 on `AGENTS.md`'s face and should have halted. Row 78 is the same shape from a different
angle — the row itself concedes the omission was "legal rather than wrong" — and row 88 inverts it: a
demonstrable gap whose *resolution* was a design choice a human in fact made, so the row records a
Type 2 decision under a Type 1 label.

The one over-broad edit is row 33. Its contradiction was real and observed (six call sites throwing on
the body swap), but the diff also edited `.claude/skills/implement-aggregate/session-c.md`, a file the
row's `Artifact` column does not name, and added two prescriptions the failure did not demonstrate:
that the upstream aggregate must already be in the state the guards require, and that "where no
upstream helper produces that state yet, add one". Both are defensible corollaries. Neither was
demonstrated, and this is exactly the shape § Step 7 warns about — a design preference riding in on
the back of a fix.

---

## Neutral-Domain Compliance

| File | Added line | Noun | harness-log row (if any) |
|------|-----------|------|--------------------------|
| — | — | — | — |

Scan run per `.claude/skills/review-artifacts/SKILL.md` § "Step 6: Check 4", scoped to the whole run.
Base `0f22736c45d001f314442385ef4007d96ec8afee`, derived by `git merge-base HEAD master`. Forbidden
nouns from `plan.md`: `Course`, `Execution`, `Question`, `Quiz`, `QuizAnswer`, `Topic`, `Tournament`,
`User`. **1619 added lines scanned across `docs/`, `.claude/skills/`, `.claude/agents/` and
`AGENTS.md`, 0 hits** — not merely zero violations after move-testing, but zero matches to test.

**Violations:** 0, of which 0 were already flagged by an aggregate-boundary `/review-artifacts` run
and not acted on, and 0 were not caught at all.

This is the run's cleanest result, and it is worth stating positively: 75 harness commits were
authored while looking directly at a quizzes domain, and not one leaked a domain noun into the
harness. Spot-checking corroborates it — row 52's fix, whose provenance is `Quiz.creationDate` vs
`availableDate`, is written entirely in `{Aggregate}` / `{startField}` placeholders.

---

## Confirmed Gaps

### G1 — the Type 1 fast path is being applied to `/review-artifacts` Check 3 findings

**Evidence:** harness-log rows 76, 78, 82, 84, 85, 86, 87, 88 (Step 7 **Misclassified** ×8)
**Step 5 verdict:** all eight rows are **Fix holds** — the gap is in the gate, not the text
**Proposed fix:** `.claude/skills/review-artifacts/SKILL.md` § "Step 7: Write the Report" should
state, per check, which harness-evolution type its findings carry: Check 2 (Pattern Alignment) and
Check 1 (path/symbol validity) produce Type 1 candidates; **Check 3 — Improvement Opportunities and
Ambiguous Guidance produce Type 2 candidates and must halt**. Add the corresponding sentence to
`AGENTS.md` § "Harness evolution": a finding whose only defect is a missing example, a
cross-reference the reader must follow, or imprecise wording is silence, not contradiction, however
obvious the improvement looks. This is ranked first because it is the only finding that bears on
whether the unilateral gate is sound, and it spans eight rows, six artifacts and two review dates.

### G2 — `docs/concepts/rule-enforcement-patterns.md` — the Decision Guide covers one class of plan.md silence and no other

**Evidence:** harness-log rows 64, 67 (and the residue of row 56)
**Step 5 verdict:** Confirmed
**Proposed fix:** § Decision Guide Step 4 is scoped by its own heading to "Unspecified out-of-domain
input to a **write** method". Two sibling silences recurred in aggregate 8 with no default at all: a
**read** functionality whose filter predicate plan.md states only in prose (row 64), and a **quantity
shortfall** a create saga cannot satisfy (row 67). Generalise Step 4 into a § "When plan.md is
silent" section with one ordered default per class — for a read predicate, prefer the reading that
makes the specified result sets partition and records the chosen predicate in plan.md's rule list; for
a quantity the saga cannot satisfy, throw on a named constant added to the rule list, never truncate
or pad silently — keeping the halt for the genuinely undecidable remainder. Structural: two aggregates,
and rows 64 and 67 both say explicitly that the harness gap was left open.

### G3 — `docs/concepts/sagas.md` — § Create Functionality Sagas specifies the create step's mechanics and nothing the created aggregate needs to exist

**Evidence:** harness-log rows 65, 66
**Step 5 verdict:** Confirmed
**Proposed fix:** two additions to § Create Functionality Sagas. (a) **Field provenance:** when one
saga creates a second aggregate, state where values the caller does not supply come from — the default
being derivation in the saga from parameters already present, with a domain sentinel for a constant,
and a signature widening only when no derivation is defensible (row 65). (b) **The compensating
delete:** item 2 requires a removal compensation whenever a later step follows, and assumes the
`Delete{Aggregate}Command` exists; say who writes it when the created aggregate's own session shipped
none, and that reopening that aggregate's session `c` for the delete is the sanctioned route rather
than reordering or leaking (row 66). A python3 scan of all 485 lines finds no text on either point.

### G4 — `docs/concepts/testing.md` — no rule for reaching a time-gated state

**Evidence:** harness-log row 68
**Step 5 verdict:** Confirmed
**Proposed fix:** the run resolved this per-application (option A of the
`2026-08-08-quiz-tournament-date-contradiction-design.md` working document: a past window is
unreachable by design, and tests reach "started"/"closed" by the passage of real time through a
fixture helper that creates a short future window and waits it out). Nothing was generalised — the row
says so, and a scan of the file for `wait`, `elapsed`, `sleep`, `gated` returns zero hits. Add a
§ "Reaching a time-gated state" to `testing.md` stating the rule: where a create path stamps a clock
field that a P1 invariant orders against a caller-supplied one, a state before that clock is
unreachable through the production path, so a test reaches it by creating a short future window and
waiting it out — never by back-dating a constant, and never by weakening the invariant.

### G5 — `.claude/skills/_shared/conventions.md` — the redirect mandate never reached the command blocks

**Evidence:** harness-log row 35
**Step 5 verdict:** **Fix incomplete**
**Proposed fix:** § "Run the test suite" :162-165 mandates `> build.log 2>&1`, but the mandated block
at :167-173 and the narrow form at :181-185 both still read `mvn ... ` / `echo "MAVEN_EXIT=$?"` with
no redirect. Since the surrounding prose calls :167-173 "the mandated form", an agent copying it gets
the un-redirected command the row was filed to eliminate. Add the redirect to both blocks. Concrete
evidence it still bites: this retrospective's own Step 3.d build had to add the redirect by hand.

### G6 — twelve `harness:` commits carry no harness-log row

**Evidence:** `53ebf1fb7`, `9c72033d0`, `f65c705da`, `94daddd93`, `fe30bd0e1`, `52e9579eb`,
`8c83170e2`, `0814cf8ac`, `44b785c8c`, `76ab595e6`, `c743e9e5e`, `998910d32`
**Step 5 verdict:** n/a — an integrity finding, not a row verdict
**Proposed fix:** `AGENTS.md` § "Harness evolution" says the harness delta of a run is exactly
`git log --oneline docs/ .claude/`, and `_shared/conventions.md` § "Harness log" makes the log the
single record of harness repair. Sixteen percent of this run's harness commits are outside it, the
largest (`76ab595e6`) covering thirteen findings at once. Add to § "Harness evolution": every
`harness:` commit carries at least one harness-log row, and a commit closing N review findings carries
N rows or one row naming all N. A cheap enforcement is a line in `/review-artifacts` asserting the
two-way match between `harness:` commits and log `Ref`s, which is the cross-check this skill performs
after the fact and nothing performs during a run.

### G7 — row 54's framework patch was reverted

**Evidence:** harness-log row 54
**Step 5 verdict:** **Fix reverted or superseded**
**Proposed fix:** none required. `73d29e458` added an `aggregateType()` filter to
`simulator/EventHandler.java`; `a13c54869` (row 55) removed it after establishing the defect was the
application's repository typing, not a framework hole. `EventHandler.java` is byte-identical to
`master` today (verified: `git diff master HEAD -- <path>` is 0 bytes) and the lesson landed in
`session-a.md` instead. This entry exists because Step 5 makes the verdict gap-producing; the episode
is the `2-fw` gate working as designed, and the harness is better off for it.

---

## Dismissed Rows

| # | Artifact | Verdict | Reason |
|---|----------|---------|--------|
| 1 | `session-a.md` | Already closed | Closed unqualified by row 7 (itself closed by row 12). **Partial in substance:** row 1 had two halves and only one closed. The verification half — the `final`-field inference being unproven — was settled by row 12's `flushAndClear()`. The prescription half was not: session-a.md still prescribes no field/constructor shape for a P1 rule classified as a Java `final` field, and its only two mentions of `final` in a P1 context (:25, :222) are exclusions. Not raised as a gap because the closure is unqualified and the run produced no friction from it after 2.1. |
| 2 | `classify-and-plan/SKILL.md` | Already closed | Closed by row 11; § 6.d now computes the saga-state set and no `READ_{AGGREGATE}` token survives. |
| 7 | `testing.md` | Already closed | Closed by row 12; testing.md:198-199 carries the unconditional `flushAndClear()` rule. |
| 9 | `session-b.md`, `session-c.md` | Already closed | Closed by row 13; session-b.md:114-116 prescribes the string literal and session-c.md's stack-trace derivation is gone. |
| 10 | `service.md` | Already closed | Closed by row 14; the `@Autowired` field is gone from the example. |
| 20 | `session-a.md` | Already closed | Closed by row 21. Note for Step 7.b: the closure **reversed** the in-slice decision — session-a.md:231-266 now names placement on the owning aggregate class, which the manager had assigned, as one of three wrong answers. |
| 23 | `commands.md` | Already closed | Closed by row 26; commands.md:63-67 carries the bulk-read rule. |
| 24 | `session-b.md` | Already closed | Closed by row 27; session-b.md:241-251 carries the foreign-id rule. |
| 25 | `testing.md` | Already closed | Closed by row 28; testing.md:505-511 carries the read-surface rule. |
| 36 | `session-c.md` | Already closed | Closed by row 39; session-c.md:173-181 scopes the negative case per test class. |
| 37 | `session-d.md` | Already closed | Closed by row 40; session-d.md:191-206 carries the two-step rule. |
| 38 | `events.md` | Already closed | Closed by row 41; events.md:79-87 documents the backlog as expected. |
| 53 | `session-c.md`, `testing.md` | Already closed | Closed by row 56; session-c.md:204 points at the Decision Guide Step 4 the closer added. |
| 71 | `quizzes-full-2-aggregate-grouping.md` | **Not a harness gap** | The named artifact is the application's own grouping spec, and Step 5 lists a spec defect explicitly. The row itself says "The circularity is in the application spec, not the harness". The re-anchoring the human chose was applied to the generated code only — the spec still anchors `QuizAnswerQuestionAnswerEvent` on `quizAnswerAggregateId` at line 100 — but that is an application-artifact divergence, out of scope for this skill. Also a misfiled row per Step 3.a. |

---

## Type 2 Halts

41 rows carry Type 2 or `2-fw`. **16 are in-session halts** where a session stopped or a manager
escalated; **13 proceeded** and were escalated later at an aggregate boundary; the remaining **12 are
closer rows**, paired below rather than counted as separate halts.

| # | Session | What the harness did not settle | What the human decided | Was the harness then fixed? |
|---|---------|--------------------------------|------------------------|-----------------------------|
| 19 | 2.2.c | T4 happy path for an op that makes its own aggregate unresolvable | Omit the case; cover by lock-acquisition, compensation and T2 | yes — `6ee9a985` |
| 31 | 2.4.b | A read filtering on an owned collection a create cannot populate | Sibling helper per write functionality; create stays minimal | yes — `bd2d61973` |
| 42 | 2.5.c | A `× N` upstream fetch: one step per element or one looping step | Single looping step; per-element fault granularity accepted as cost | yes — `62c3cf6bb` |
| 48 | 2.5 | Whether the planner can see that a saga will hold a lock | List the pair unconditionally with a gate note deferring to session `c` | yes — `cd81011ba` |
| 49 | 2.5 | Whether `/review-artifacts` should read `*.template` files | Widen the set, plus a claimed-symbol sub-table | yes — `4cddc5020` |
| 50 | 2.5 | `loadForCheck` ships in every scaffold and is named by no doc | Document it as the sanctioned aggregate read-back | yes — `ec99396dc` |
| 53 | 2.7.c | plan.md silent on an out-of-domain input to a write method | Add the `QUESTION_NOT_IN_QUIZ_ANSWER` guard | yes — via row 56, `475e157ef` |
| 54 | 2.7.d | `EventHandler` handing a consumer foreign aggregates' ids | Framework filter on a new `aggregateType()` | yes — `73d29e458`, later reversed by row 55 |
| 55 | 2.7.d | Whether that filter was the right fix at all | Revert it; fix the application's repository typing instead | yes — `a13c54869` |
| 64 | 2.8.b | plan.md silent on a read functionality's filter predicate | `open = !cancelled && endTime > now`; `closed = !cancelled && endTime <= now` | **no** |
| 65 | 2.8.c | Provenance of quiz fields `CreateTournament` does not supply | Derive in the saga; new `TOURNAMENT_QUIZ_TITLE` sentinel | **no** |
| 66 | 2.8.c | Who writes the compensating delete when the aggregate has none | Add `QuizService.deleteQuiz` + command + handler case, reopening 2.6.c | **no** |
| 67 | 2.8.c | No constant for a question-count shortfall | Throw on a new `TOURNAMENT_NOT_ENOUGH_QUESTIONS`, guarded in the service | **no** |
| 68 | 2.8.c | How a test reaches a time-gated state the create path cannot build | A past window is unreachable by design; create a short future window and wait it out | **no** |
| 71 | 2.8.d | An event anchor the app spec makes unconstructible | Re-anchor on `quizAggregateId`, discriminate by `studentAggregateId` | **no** |
| 73 | review | How far to widen the review set beyond `.claude/` | `AGENTS.md` only, with a stated inclusion criterion | yes — `bdc39dbb0` |

Ten of sixteen halts produced a harness edit. **All six that did not are in aggregate 8**, and five of
those six rows end with the same sentence — "Harness gap itself left open - no docs/skills edit made
this session." The gate worked every time it was reached; what the run stopped doing, in its final
aggregate, was converting the human's answer into harness text. That is the single clearest reason
this retrospective returns "did not converge" rather than "converged with gaps".

**Proceeded without halting:** rows 1, 2, 9, 10, 20, 23, 24, 25, 30, 36, 37, 38 — twelve rows where
the gate was available and not used, plus row 7. Each was recorded and carried, then escalated at the
next aggregate boundary and closed by a human decision, so none went unanswered; the cost was that the
code was written first and ratified after. Row 1 inferred a JPA shape for a `final`-field P1 rule and
left it unproven. Row 2 guessed a saga-state enum by scanning later aggregates' plan.md sections.
Rows 9 and 10 matched the existing file rather than introduce a second idiom. Rows 23, 24 and 25
resolved by analogy to an in-app precedent and explicitly "did not halt". Row 30 read a collection
helper as a setter. Rows 36, 37 and 38 were raised at the boundary rather than mid-session, row 37
stating "Proceeded without halting" outright.

**Row 20 deserves separate mention as the one gate breach in the run.** Its slice reported it *would*
have halted on where a domain sentinel lives; the manager pre-empted the halt by assigning the
constant to the owning aggregate class in the slice brief. The row records this honestly — "the
decision stands but was not the manager's to make". The human's later ruling (row 21) then **reversed
it**: `session-a.md:231-266` now lists placement on the owning aggregate class as one of three
defensible-looking placements that are wrong. The one time the Type 2 gate was bypassed, the bypassed
decision turned out to be the wrong one.

---

## Recorded Limitations

`applications/quizzes-full-2/plan.md` carries **no Recorded Limitations section** — it has no such
heading, and this retrospective therefore has none to carry forward. That is itself a limitation on
what this report can claim, and the following confounds were reconstructed from the run's own history
rather than from a declaration:

- **The reference applications were deliberately stripped before the run** (`05944e064`, "chore: strip
  reference applications for the quizzes-full-2 controlled regeneration"). Friction counts here are
  therefore not comparable with a run in which sibling applications were available to read.
- **24 of 89 rows (27%) were produced by static `/review-artifacts` sweeps, not by implementing
  sessions**, and 23 of those are Type 1. They are excluded from the convergence curve. Their volume
  is a function of how often the sweeps were run and how hard they looked, which varied across the
  run — four review reports landed on 2026-08-10 alone — so the Type 1 total is not a clean measure
  of how much the harness misled an implementing agent.
- **Delegated execution was introduced mid-run.** Aggregates 2, 4, 7 and 8 record per-slice sessions
  in plan.md while 1, 3, 5 and 6 do not, so friction attributable to slicing (rows 34, 36, 47) could
  only arise in some aggregates.
- **Seven rows' `Ref`s were invalidated by a history rewrite**, so seven Step 7 verdicts rest on
  commits matched by identical subject rather than on the sha the logging agent wrote. The match is
  unambiguous in all six cases, but it is an inference.
- **Aggregate 8 is the only aggregate whose gaps were left open**, and it is also the last. A run that
  ended at aggregate 7 would have shown convergence on this report's own criteria. Whether aggregate 8
  is genuinely harder — it has nine subscribed events, the most of any aggregate, and depends on five
  upstream aggregates — or whether the run simply stopped generalising once the end was in sight, this
  evidence cannot distinguish.
