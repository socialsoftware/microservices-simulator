# Harness Retrospective - trainticket

**App:** trainticket
**Date:** 2026-08-23
**Harness-log rows:** 24 (Type 1 15 / Type 2 9 / 2-fw 0)
**Sessions with friction:** 15 of 26 (24 Phase 2 sessions + Phase 0 + Phase 1)
**Harness commits:** 18 referenced by the log (17 under `docs`/`.claude/skills`, plus `395d4ace2` on `AGENTS.md`)
**Verdict:** Harness converged with gaps

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
| Sessions executed | 26 (Phase 0, Phase 1, 24 Phase 2 sessions `2.1.a` – `2.8.c`) |
| Harness-log rows | 24 (T1 15 / T2 9 / 2-fw 0) |
| Outcomes | fixed 19 / declined 0 / deferred (open) 4 / deferred (closed) 1 |
| `harness:` commits on the run | 18 referenced by the log; 42 commits total touch `docs`/`.claude/skills` between `0f22736c4` and `HEAD` (see integrity note) |
| Type 2 halts | 3 human decisions (rows 2, 5, 7; row 9 resolved under row 7's decision) |
| Build outcome | MAVEN_EXIT=0, tests=290 failures=0 errors=0 skipped=0 |

This run exercised the harness against a materially harder spec than a warm-up: 8 aggregates, 16
cross-entity rules, an aggregate owning a collection of entities (`Route`), a composite-key read
(`GetPriceConfigByRouteAndTrainType`) and an aggregate with five domain-specific mutating operations
(`Order`). It produced 24 friction rows, 19 of them repaired in-session, and finished with a green
suite of 290 tests. The Type 1 audit (Step 7) found no edit that ratified a guess, so the unilateral
gate held. What did not converge is the Type 2 side: the harness kept going silent at roughly one new
place per aggregate all the way to `PriceConfig` (aggregate 7), and four of those silences are still
open. Two structural findings sit outside the row-by-row evidence and matter more than any single
gap: the aggregate-boundary `/review-artifacts` checkpoint was skipped for aggregates 1–7 and run
only twice at the end, where it found three Critical defects and produced 22 further `harness:`
commits - all of which were live, uncorrected guidance while sessions `2.2.*` through `2.8.*` ran;
and five of the nine Type 2 rows did not halt at all, using `deferred` as a third gate that
`AGENTS.md` does not define.

**Harness-log integrity:**

- Numbering `1`–`24`, monotonic, no gaps, no duplicates.
- Every `Type` is `1` or `2`; no `2-fw` row; no Type 1 row names an artifact under `simulator/`.
- Every `Outcome` is `fixed` or `deferred`; no `declined` rows.
- Every `fixed` row carries a resolvable `Ref`; all 18 shas exist and all 18 diffs were read.
- **Row 2's `Artifact` is `AGENTS.md`**, which is neither under `docs/`, `.claude/skills/` nor
  `simulator/` - the three paths `conventions.md` § "Harness log" enumerates. It is plainly a harness
  file, not an implementation defect, so this is a schema gap, not a misfiled row (gap **G5**).
- **25 of the 42 commits in `git log 0f22736c4..HEAD -- docs .claude/skills` are referenced by no
  row.** They break down as: 2 `docs:` report commits (`824efbc51`, `a2079151c`, not `harness:`
  commits); 1 pre-`/boot-strap` harness commit (`66acbd961`, exempt - `conventions.md` § "Harness
  evolution" scopes the gates from `/boot-strap` onward); and 22 post-run `harness:` commits from the
  two `/review-artifacts` passes, whose findings go to the review report's Action Items rather than
  the harness log. None is an unrecorded edit, but the Step 3.a.i cross-check as written flags all 25
  (gap **G6**).
- `395d4ace2` (row 2) does **not** appear in that listing, because the delta pathspec `docs .claude/`
  does not cover `AGENTS.md` - the other half of **G5**.
- Retro reconciliation is clean: all 24 retros carry a `## Harness Changes` sub-table, every row
  number in them matches the log, every session with rows has a retro naming them, and no retro
  carries a stale `Action Items` table.

---

## Rows by Artifact

| Artifact | Rows | T1 | T2 | 2-fw | fixed | declined | deferred (open) | deferred (closed) | What the friction was about |
|----------|------|----|----|------|-------|----------|------------------|--------------------|-----------------------------|
| `.claude/skills/implement-aggregate/session-a.md` | 7 (5, 6, 12, 15, 16, 17, 23) | 3 | 4 | 0 | 4 | 0 | 3 | 0 | The domain-layer spec: shared enums, `@Entity` on the concrete saga class, `final` P1 fields, enum-field JPA mapping, owned-collection mapping and deep copy, saga-state naming |
| `docs/concepts/commands.md` | 3 (7, 20, 22) | 1 | 2 | 0 | 2 | 0 | 0 | 1 | Which step shapes have no `rootAggregateId` to pass - unfiltered read, filtered read, composite-key read |
| `.claude/skills/implement-aggregate/session-c.md` | 3 (10, 19, 24) | 3 | 0 | 0 | 3 | 0 | 0 | 0 | Write-session test obligations: who owns the compensation test, and what a P4a violation test can assert |
| `.claude/skills/implement-aggregate/session-b.md` | 3 (9, 13, 18) | 2 | 1 | 0 | 3 | 0 | 0 | 0 | Read-session artifacts: collection-read not-found case, bean wiring, fixture defaults for foreign ids |
| `docs/concepts/testing.md` | 3 (11, 14, 21) | 2 | 1 | 0 | 2 | 0 | 1 | 0 | T4 state assertion on a soft-delete, capturing the compensation-test log line, BVA tick for a decimal |
| `.claude/skills/classify-and-plan/SKILL.md` | 2 (3, 4) | 2 | 0 | 0 | 2 | 0 | 0 | 0 | Parsing the §3.2 rule blocks and rooting the Step 7 file-list paths |
| `docs/concepts/aggregate.md` | 1 (12) | 0 | 1 | 0 | 0 | 0 | 1 | 0 | `final` field vs JPA hydration and the copy constructor |
| `.claude/skills/boot-strap/SKILL.md` | 1 (1) | 1 | 0 | 0 | 1 | 0 | 0 | 0 | Step 5 in a linked git worktree |
| `AGENTS.md` | 1 (2) | 0 | 1 | 0 | 1 | 0 | 0 | 0 | The required JDK was stated nowhere |
| `docs/concepts/sagas.md` | 1 (7) | 0 | 1 | 0 | 1 | 0 | 0 | 0 | No saga template for an unfiltered collection read |
| `docs/concepts/service.md` | 1 (8) | 1 | 0 | 0 | 1 | 0 | 0 | 0 | Bulk-read query not declared on the abstract custom repository |
| `.claude/skills/implement-aggregate/SKILL.md` | 1 (10) | 1 | 0 | 0 | 1 | 0 | 0 | 0 | Session→test-file table omitted the compensation test |
| `.claude/skills/_shared/conventions.md` | 1 (14) | 1 | 0 | 0 | 1 | 0 | 0 | 0 | No stdout-capture mechanism surviving the output-filtering hook |

**`.claude/skills/implement-aggregate/session-a.md` is the harness's largest single weakness for
this run**: 7 of 24 rows, across Phase 1 and aggregates 1, 2, 3, 4 and 8, and it is the only artifact
carrying more than one open gap. Three of its four Type 2 rows (12, 15, 17) are still open, and all
three are JPA-mapping questions the build cannot catch - a `final` field, an `@Enumerated` default,
and a shared entity instance across aggregate versions. Its Type 1 rows were absorbed cleanly; its
silences were not.

## Rows by Type

| Type | Rows | Notes |
|------|------|-------|
| 1 | 15 (1, 3, 4, 6, 8, 10, 11, 13, 14, 16, 18, 19, 22, 23, 24) | All fixed in-session. Ten were self-contradictions or cross-file disagreements inside the harness (3, 4, 8, 10, 11, 13, 14, 19, 22, 23); five were statements that failed mechanically against the framework or the toolchain (1, 6, 16, 18, 24). Step 7 audits every one. |
| 2 | 9 (2, 5, 7, 9, 12, 15, 17, 20, 21) | Only rows 2, 5 and 7 (with 9 riding on 7's decision) actually halted for a human. Rows 12, 15, 17, 20 and 21 recorded a silence, chose a reading and proceeded - `deferred`. Row 20 was later closed by row 22; the other four are open. |
| 2-fw | 0 | No agent ever concluded the framework itself was at fault. `simulator/` was untouched for the whole run. |

---

## Convergence

| Aggregate / phase | Sessions | Type 1 | Type 2 | 2-fw | Total |
|-------------------|----------|--------|--------|------|-------|
| Phase 0 (boot-strap) | `0` | 1 | 1 | 0 | 2 |
| Phase 1 (classify-and-plan) | `1` | 2 | 1 | 0 | 3 |
| 1. Station | `2.1.a` `2.1.b` `2.1.c` | 4 | 2 | 0 | 6 |
| 2. TrainType | `2.2.a` `2.2.b` `2.2.c` | 2 | 1 | 0 | 3 |
| 3. User | `2.3.a` `2.3.b` `2.3.c` | 0 | 1 | 0 | 1 |
| 4. Route | `2.4.a` `2.4.b` `2.4.c` | 3 | 1 | 0 | 4 |
| 5. Contacts | `2.5.a` `2.5.b` `2.5.c` | 0 | 1 | 0 | 1 |
| 6. Trip | `2.6.a` `2.6.b` `2.6.c` | 0 | 0 | 0 | 0 |
| 7. PriceConfig | `2.7.a` `2.7.b` `2.7.c` | 1 | 1 | 0 | 2 |
| 8. Order | `2.8.a` `2.8.b` `2.8.c` | 2 | 0 | 0 | 2 |

**Pattern: declining, but not to zero - and the decline is entirely in Type 1.**

Aggregates 1–4 produced 14 rows; aggregates 5–8 produced 5. That is a real decline and the run is
long enough (8 aggregates) for it to mean something. But the count never reaches zero and stays
there: aggregate 6 (`Trip`) is the only clean aggregate, and it is followed by 2 and 2.

Read by type, the two halves behave differently and the difference is the finding:

- **Type 1 declined with two spikes, both explained by a first-of-kind shape.** Station (4) is the
  cold start. Route (3) is the first aggregate owning a collection of entities. Order (2) is the
  first with domain-specific mutating operations and a composite-key upstream fetch. Between them,
  User, Contacts and Trip produced zero Type 1 rows - the harness genuinely absorbed the earlier
  lessons and those three aggregates traversed it cleanly.
- **Type 2 was flat at one per aggregate through aggregate 7** (2, 1, 1, 1, 1, 0, 1, 0). This is the
  pattern the skill exists to name: the harness's factual errors were cleared, its silences were not,
  because a silence is only ever filled by a human decision and five of the nine were never put to a
  human.

**Structural artifacts - those recurring across aggregates:**

- `.claude/skills/implement-aggregate/session-a.md` - Phase 1 + aggregates 1, 2, 3, 4, 8. Three of
  its recurrences (rows 12, 15, 17) are JPA-mapping questions that compile and persist either way, so
  the build can never converge them; only a doc edit can.
- `docs/concepts/commands.md` - aggregates 1, 5, 7, all three the same question (which step shapes
  pass no root aggregate id). Row 22 finally replaced the closed enumeration with the governing
  property, which is the fix that should stop the recurrence.
- `.claude/skills/implement-aggregate/session-c.md` - aggregates 1, 4, 8, all about what a P4a
  violation test may assert. Row 24 refines row 19's carve-out; a third upstream not-found path would
  produce a fourth row.

---

## Post-Hoc Justification of the Type 1 Edits

(This section goes into the run's written evaluation verbatim. It is the audit owed by a gate that
lets an agent edit the harness without asking.)

| # | Session | Artifact | Claimed contradiction | Demonstrable? | Verdict |
|---|---------|----------|----------------------|---------------|---------|
| 1 | 0 | `boot-strap/SKILL.md` | Step 5's fallback chain and its `>> .git/info/exclude` both fail in a linked worktree, where `.mvn/maven.config` is absent and `.git` is a file | Yes - `cp` from a non-existent source and a redirect into a path that is not a directory both fail outright; `git rev-parse --git-common-dir` is the documented fix | Sound |
| 3 | 1 | `classify-and-plan/SKILL.md` | The §3.2 rule-block regex matches neither this spec nor the shape `domain-model-template.md` § 3.2 prescribes | Yes - 0 matches on 16 blocks, and the template demonstrably interposes the `\| Field \| Value \|` header rows the pattern forbids | Sound |
| 4 | 1 | `classify-and-plan/SKILL.md` | Step 7 declared one path root and named `ServiceMapping.java` the sole exception, contradicting `workflow.md` § "Package layout" and `commands.md` § "File Location" | Yes - two harness files prescribing different roots for `commands/` and `events/` | Sound |
| 6 | 2.1.a | `session-a.md` | The `Saga{Aggregate}` spec omits `@Entity` on the only concrete class in a `TABLE_PER_CLASS` hierarchy | Yes - failing build: *Unable to locate persister: …SagaStation* on the first persist | Sound |
| 8 | 2.1.b | `docs/concepts/service.md` | The bulk-read query is routed only to the JPA repo and the concrete `Sagas` class, while `AGENTS.md` § Architecture principle and § "Injected Dependencies" make the service inject the abstract interface | Yes - does not compile, and two harness files disagree | Sound |
| 10 | 2.1.c | `implement-aggregate/SKILL.md`, `session-c.md` | `testing.md` § T4 requires `{Op}CompensationTest`; no session's file list produced it | Yes - a required artifact owned by no session. The added skip condition faithfully restates `testing.md` § Compensation Test's own "unconditional, non-fault reason" carve-out rather than inventing one | Sound |
| 11 | 2.1.c | `docs/concepts/testing.md` | § T4 mandates `sagaStateOf(id) == NOT_IN_SAGA` on every write happy path, while the same doc's § T3 filtering rule makes the aggregate unloadable after a soft delete | Yes - the assertion throws `SimulatorException` instead of comparing; the contradiction is internal to one file. The substitute assertion shape is copied from § T3's existing deletion-event pattern, not invented | Sound |
| 13 | 2.2.b | `session-b.md` | The bean template passes five constructor arguments to a service `service.md` § "Injected Dependencies" gives three, with the factory and id generator as `@Autowired` fields | Yes - does not compile; verified against the current `service.md` example, which still shows the three-argument constructor | Sound |
| 14 | 2.2.c | `docs/concepts/testing.md`, `_shared/conventions.md` | § Compensation Test requires confirming a `START EXECUTION STEP` console line; § "Run the test suite" forbids reading maven stdout and names no surviving mechanism | Yes - two skills prescribing incompatible things, and the redirect was observed to yield a condensed transcript with zero matches | Sound |
| 16 | 2.4.a | `session-a.md` | A plain `@OneToMany` is described as "the inverse side with no FK column"; in JPA that is the owning side mapped through a join table, and the named entity has no back-reference for a `mappedBy` | Yes - a factual claim about JPA that is false, and the `mappedBy` it invites has no target to name | Sound |
| 18 | 2.4.b | `session-b.md` | The fixture must both default every argument to a domain constant and survive session `c`'s reroute onto the real create saga; for an aggregate whose create saga fetches a foreign aggregate, both cannot hold | Yes - the placeholder id resolves to nothing and the data-assembly fetch throws. **But** the diff also prescribes making the foreign-id parameter overridable rather than computed inside the helper - a fixture-design preference the contradiction does not force, and one no failing build demonstrates | Sound but over-broad |
| 19 | 2.4.c | `session-c.md` | The section's own P4a-prerequisite bullet covers a rule enforced by the upstream fetch, which raises `SimulatorException` and has no rule constant, so its own mandated `ex.message == {RULE_NAME}` assertion cannot be written | Yes - self-contradiction within one section of one file | Sound |
| 22 | 2.7.b | `docs/concepts/commands.md` | § "What a Command Is" states a closed enumeration of two no-root-aggregate shapes that contradicts the governing property stated in the same block three lines below it | Yes - self-contradiction within one paragraph; the composite-key read satisfies the property but is excluded by the enumeration | Sound |
| 23 | 2.8.a | `session-a.md` | § `{Aggregate}SagaState.java` says a simple two-step write saga needs only `READ_{AGGREGATE}`, while `sagas.md` § "Lock-Acquisition Step Pattern" and § "Step Ordering" item 3 require the lock step for exactly that shape | Yes - two harness files prescribing different things. The `IN_{OPERATION}_{AGGREGATE}` naming the diff adds is not invented: `session-c.md` already read "each `setSemanticLock` step is an *acquire* transition into `IN_{OP}`" at that commit, so the edit propagates an existing rule rather than settling a silence | Sound |
| 24 | 2.8.c | `session-c.md` | Row 19's carve-out assumed the upstream fetch is always a primary-key load; a composite-key fetch throws `{AppClass}Exception` with the upstream's own constant, so neither the carve-out's `SimulatorException` nor the general rule's non-existent rule constant could be asserted | Yes - the carve-out itself fails on the case in front of the agent | Sound |

**Verdict ratio:** Sound 14 / Sound but over-broad 1 / Ratified a guess 0 / Misclassified 0

Fourteen of fifteen Type 1 edits fixed exactly the contradiction they claimed, and every claim is
checkable today: the two named files genuinely disagreed at that commit, or the build genuinely
failed. No edit ratified a guess - nothing in this run's Type 1 set is an agent editing a doc to
agree with code it had already written, and in particular the two edits with the most room for that
(row 11's substitute assertion, row 23's naming rule) both traced their remedy to text that already
existed elsewhere in the harness. On this run the unilateral gate was safe.

The single over-broad edit (row 18) is the textbook shape and worth naming as such: a real
contradiction about defaulted foreign ids carried a second, unforced prescription about parameter
overridability into `session-b.md`. It is harmless here - nothing in the run depends on it - but it
is exactly how a design preference rides in on the back of a demonstrable fix, and it is the one edit
in this set a human never saw and never agreed to.

**A separate gate observation, outside the Type 1 audit.** Five of the nine Type 2 rows (12, 15, 17,
20, 21) did not halt. `AGENTS.md` § "Harness evolution" says Type 2 means "**Halt and ask the human
before writing the code**"; these five recorded the silence, chose a reading, wrote the code and
marked the row `deferred`. Four are still open. The outcome was benign - the readings chosen are all
defensible and the suite is green - but `deferred` is functioning as a third gate the harness does
not define, and it is the mechanism by which the Type 2 rate stayed flat: a silence that never
reaches a human is never filled. See gap **G7**.

---

## Neutral-Domain Compliance

Run per `.claude/skills/review-artifacts/SKILL.md` § "Step 6: Check 4 - Neutral Domain", scoped to
the whole run.

**Base commit:** `0f22736c45d001f314442385ef4007d96ec8afee`, from `git merge-base HEAD master`. It
did **not** degenerate to `HEAD` (`HEAD` is `f718ed985` on branch `trainticket`), so the
`harness:`-commit fallback was not needed.
**Added lines scanned:** 556 (`git diff 0f22736c4 -- docs .claude ':(exclude)docs/reviews' -M`,
`^+` lines, `+++` headers excluded) - the whole-run scope, covering every `harness:` commit including
the two post-run `/review-artifacts` batches.
**Forbidden nouns:** `Contacts`, `Order`, `PriceConfig`, `Route`, `Station`, `TrainType`, `Trip`,
`User` (one per `### {N}. {Aggregate}` header in `applications/trainticket/plan.md`), plus
`trainticket` / `TrainTicket` / `Trainticket` and the domain words `ticket`, `seat`, `train`.

| File | Added line | Noun | harness-log row (if any) |
|------|-----------|------|--------------------------|
| `.claude/skills/_shared/conventions.md` | `If there is exactly one result, use it. If there are several, apply this tie-break in order - \`find\`` | `order` (case-insensitive) | - (commit `ff9001e9b`, review lane) |
| `.claude/skills/_shared/conventions.md` | `returns them in directory order, which has nothing to do with which run is current, so never just take` | `order` (case-insensitive) | - (commit `ff9001e9b`, review lane) |

Both hits are ordinary English usage of "order" and are **false positives**; neither names the
`Order` aggregate. The case-sensitive whole-word pass returned zero hits across all 556 added lines.

**Violations: 0**, of which 0 were already flagged by an aggregate-boundary `/review-artifacts` run
and not acted on, and 0 were not caught at all. Both end-of-run `/review-artifacts` passes
independently reported 0 violations over their own (narrower) scopes - 372 and 513 added lines
respectively - and this whole-run scan over 556 lines agrees. The neutral-domain rule held completely
on this run, which is notable given that six of the eight forbidden nouns (`Order`, `Route`,
`Station`, `User`, `Trip`, `Contacts`) are ordinary English words an author reaches for by reflex.

---

## Confirmed Gaps

No **Fix wrong** row and no **Ratified a guess** verdict, so priority band 1 is empty. No open `2-fw`
row, so band 3 is empty. G1–G3 are band 2 (evidenced across more than one aggregate); G4–G8 are
band 4.

### G1 - `.claude/skills/implement-aggregate/session-a.md`, `docs/concepts/aggregate.md` - no pattern for a Java-`final` aggregate field under JPA hydration and the mandated copy constructor

**Evidence:** harness-log row 12 (session `2.2.a`); recurrence recorded in `retro-2.3.a-User.md`
("Still no statement of how a Java-`final` aggregate field coexists with JPA hydration and the
mandated copy constructor (open row 12)") and predicted in row 12 for Trip, User and Order - four
aggregates in total, five counting Order's twelve frozen contract fields.
**Step 5 verdict:** Confirmed. A fresh read of `session-a.md` finds no occurrence of `final` in the
`{Aggregate}.java` section (the only hits are `public static final String` error constants at L222
and domain constants at L246), and `docs/concepts/aggregate.md` contains no occurrence of `final` or
`hydrat` at all.
**Proposed fix:** Add a bullet to `session-a.md` § "`{Aggregate}.java`", under the field list, stating
that a P1 immutability rule is realised as a Java `final` field with no setter, assigned in all three
constructors (creating, copy, and the JPA no-arg constructor), and that Hibernate 6 uses field access
and round-trips it - so no `@Access` annotation and no mutable shadow field is needed. Row 12 records
that this was settled mechanically with a persistence probe; state the result, not the probe.
`docs/concepts/aggregate.md` § Base Class should carry the same statement once and `session-a.md`
should point at it, so the rule has one owner.

### G2 - `.claude/skills/implement-aggregate/session-a.md` - § "Domain enums" does not state the JPA mapping of an enum-typed aggregate field

**Evidence:** harness-log row 15 (session `2.3.a`); recurrence recorded in `retro-2.5.a-Contacts.md`,
which explicitly declines to open a second row because row 15 predicted it. Row 15 names Contacts,
Trip, PriceConfig and Order as further recurrences.
**Step 5 verdict:** Confirmed. `session-a.md` § "Domain enums" exists at L79 and contains no
occurrence of `Enumerated`.
**Proposed fix:** One line at the end of `session-a.md` § "Domain enums": an aggregate field typed by
a domain enum carries `@Enumerated(EnumType.STRING)`, never JPA's `ORDINAL` default, so stored rows
survive a reordering of the enum constants. This gap is invisible to the build - both mappings
compile and persist - which is precisely why it needs a doc and cannot converge on its own.

### G3 - `.claude/skills/implement-aggregate/session-a.md` - no deep-copy rule for an **owned collection** in the aggregate copy constructor

**Evidence:** harness-log row 17 (session `2.4.a`), which names it as recurring "for any later
aggregate owning a collection"; `retro-2.4.a-Route.md` rates it High.
**Step 5 verdict:** Confirmed. The only deep-copy guidance in `session-a.md` is at L77, scoped to the
nested entity-to-entity `@OneToOne` case; the collection case has none.
**Proposed fix:** Restate the deep-copy rule in `session-a.md` § "Owned entity classes" as
cardinality-independent: the copy constructor constructs a new owned entity per element
(`new {Entity}(existing)`), never reuses instances across aggregate versions. State the consequence
explicitly - with `orphanRemoval = true`, sharing an instance means the previous version's collection
is emptied when the new version is committed, a silent data-loss bug no test in the mandated matrix
would catch. Only one aggregate exercised it this run, but the failure mode is the most severe of the
four open gaps.

### G4 - `docs/concepts/testing.md` - § "Choosing Input Values - EP & BVA" gives no boundary tick for a fixed-scale decimal

**Evidence:** harness-log row 21 (session `2.7.a`), which names it as recurring "for any later
aggregate with a `BigDecimal` positivity rule".
**Step 5 verdict:** Confirmed. `testing.md` L106 gives the `LocalDateTime` tick (`.minusNanos(1)`)
and the integer tick; the file contains no occurrence of `BigDecimal` or `scale`.
**Proposed fix:** Add a row to the § "Worked patterns" table for `{amount} > 0` over a fixed-scale
decimal: the on-point is the smallest value the persisted column's scale can round-trip
(`10^-scale`), because an arbitrary-precision decimal has no smallest positive value in the domain
itself. Name the column's `scale` attribute as the source, so the value is derived rather than
guessed per aggregate.

### G5 - `.claude/skills/_shared/conventions.md` § "Harness log" and `AGENTS.md` § "Harness evolution" - the harness's own file set excludes `AGENTS.md`

**Evidence:** harness-log row 2, whose `Artifact` is `AGENTS.md`. Its own fix (`395d4ace2`) **holds**
- the JDK 21 requirement is present in `AGENTS.md` § "Build Commands" today - so row 2 is dismissed
on its own terms; the gap is in the schema that had nowhere to put it.
**Step 5 verdict:** Confirmed (structural). `conventions.md` § "Harness log" says "Only findings that
target `docs/`, `.claude/skills/` or `simulator/` come here", which makes a legitimate `AGENTS.md`
row unschematic; `AGENTS.md` § "Harness evolution" defines the run's harness delta as
`git log --oneline docs/ .claude/`, which omits `AGENTS.md` itself - so `395d4ace2` is invisible to
the delta, to `/review-artifacts` Check 4's diff, and to this skill's Step 3.a.i cross-check.
**Proposed fix:** In `conventions.md` § "Harness log", change the enumerated set to `AGENTS.md`,
`docs/`, `.claude/` and `simulator/`. In `AGENTS.md` § "Harness evolution", change the delta
definition to `git log --oneline AGENTS.md docs/ .claude/`. Propagate the same pathspec to
`review-artifacts/SKILL.md` § Step 6.b and to `harness-retrospective/SKILL.md` § Step 3.a.i.

### G6 - `.claude/skills/harness-retrospective/SKILL.md` § Step 3.a.i - the commit cross-check assumes every harness commit in the window has a log row

**Evidence:** 25 of the 42 commits returned by the prescribed listing are referenced by no row, and
none of the 25 is an unrecorded edit: 22 belong to the `/review-artifacts` lane (whose findings go to
the review report's Action Items by design), 2 are `docs:` report commits, and 1 (`66acbd961`)
predates `/boot-strap`, which `conventions.md` § "Harness evolution" explicitly exempts.
**Step 5 verdict:** Confirmed (structural - found by running the skill, not by a log row).
**Proposed fix:** In § Step 3.a.i, replace "every commit in the listing must be referenced by some
row" with a three-way partition: commits referenced by a log row; commits belonging to the
`/review-artifacts` lane, identified by falling after the run's last session commit or by being named
in a `reviews/review-*.md` Action Items table; and commits predating the `/boot-strap` commit.
Only a commit in none of the three is an unrecorded edit and a finding. As written, the check
produces 25 false findings on a normal, well-run session and trains the reader to skip the section.

### G7 - `AGENTS.md` § "Harness evolution" and `conventions.md` § "Harness log" - `deferred` is an undefined third gate

**Evidence:** harness-log rows 12, 15, 17, 20 and 21 - five of the nine Type 2 rows - all marked
`deferred`, all with the agent proceeding on a self-chosen reading rather than halting. Four are
still open at the end of the run (G1–G4 are exactly those four).
**Step 5 verdict:** Confirmed. `AGENTS.md` § "Harness evolution" states Type 2 means "Halt and ask
the human before writing the code", and defines no exception; `conventions.md` § "Harness log" lists
`deferred` as an allowed `Outcome` and defines only `declined` ("the human decided the harness was
right"), never `deferred` itself. The two together let a Type 2 be recorded and bypassed without a
human, which is what kept the Type 2 rate flat across seven aggregates.
**Proposed fix:** Define `deferred` explicitly in `conventions.md` § "Harness log" and reconcile it
with the gate in `AGENTS.md`. The honest definition of what happened here is: *the silence did not
block the code, a defensible reading was available and taken, and the design decision is owed to a
human but not owed now*. If that is the intended semantics, say so in `AGENTS.md` § "Harness
evolution" as a stated narrowing of the halt rule, with the condition that makes it legitimate (the
readings are indistinguishable to the build, so no code is at risk) and the obligation it creates
(the row must be closed before the next run starts). If it is not the intended semantics, `deferred`
should be removed as an `Outcome` and these five rows should have halted.

### G8 - `.claude/skills/_shared/session-completion.md` retro template - the `## Documentation Gaps` table has no route into the harness log or a fix queue

**Evidence:** across the 24 retros, 37 distinct documentation gaps are recorded in
`## Documentation Gaps` tables. Sixteen correspond to a harness-log row (fixed or deferred);
**21 correspond to none**, and **11 of those 21 are rated Medium or High by their own author**. Four
were re-checked against the current artifacts and are all still open: `docs/concepts/aggregate.md`
mentions `prev` only as a version pointer and carries no section on invariants that read it
(`retro-2.8.a-Order.md`, Medium); `docs/concepts/rule-enforcement-patterns.md` still defines P4b as
"same value is passed to two aggregates in the same saga" (`retro-2.8.c-Order.md`, Medium);
`docs/concepts/sagas.md` § "Step Ordering" contains no occurrence of a collection-valued prerequisite
or a looping data-assembly step (`retro-2.4.c-Route.md`, Medium); and `docs/concepts/testing.md` § T1
contains no occurrence of `prev`/`getPrev` and so has no template for a P1 rule predicating on the
previous version (`retro-2.8.a-Order.md`, Medium). Two of the 11 have the highest stakes:
`retro-2.1.c-Station.md` rates `commands.md` § "Sending Commands" **High** for calling
`setForbiddenStates` on a plain `Command` when the method is declared on `SagaCommand`, and
`retro-2.8.c-Order.md` records that deriving a P4b value in the workflow rather than the service can
make a P3 guard unreachable and surface an NPE in place of the rule's exception.
**Step 5 verdict:** Confirmed (structural - surfaced by the run, fixed by nobody).
**Proposed fix:** The retro template should require each `## Documentation Gaps` row rated Medium or
High to either carry a harness-log row number or state in one clause why it is neither Type 1 nor
Type 2. Separately, `/harness-retrospective` § Step 3.b should aggregate the unrouted rows into the
gap list (this report does so by hand). Without one of the two, the retros are a well-kept record
that nothing reads: 21 author-recorded defects in the harness, 11 of them rated Medium or High,
survived a run that was explicitly designed to find and repair exactly those.

---

## Dismissed Rows

| # | Artifact | Verdict | Reason |
|---|----------|---------|--------|
| 1 | `.claude/skills/boot-strap/SKILL.md` | Fix holds | Current Step 5 derives `root`/`common`/`main` via `git rev-parse --show-toplevel` and `--git-common-dir`, searches both worktrees, and appends to `"$common/info/exclude"`. All 24 added lines present verbatim. |
| 2 | `AGENTS.md` | Fix holds | `AGENTS.md` § "Build Commands" now opens with "**JDK 21 is required.**" and gives the `sdk use java 21.0.10-tem` / `JAVA_HOME=` forms. All 6 added lines present. (Cited as evidence for G5, which is about the schema, not this fix.) |
| 3 | `.claude/skills/classify-and-plan/SKILL.md` | Fix holds | § 2.b now carries the template block, the qualifier rule and the broadened regex. One added line is absent only because `7d0babce6` replaced its em dash with a plain dash; the sentence is intact. |
| 4 | `.claude/skills/classify-and-plan/SKILL.md` | Fix holds | Step 7 now opens "Paths in the tables below resolve against **three** roots" and enumerates app source root, app test root and aggregate package. All 10 added lines present. |
| 5 | `.claude/skills/implement-aggregate/session-a.md` | Fix holds | The shared-enum rule (one enum at `{src}enums/`, imported by every aggregate naming it) is present; all 21 added lines survive. Human-decided (Type 2 halt). |
| 6 | `.claude/skills/implement-aggregate/session-a.md` | Fix holds | § `Saga{Aggregate}.java` now leads with "Annotated `@Entity`" and states the `TABLE_PER_CLASS` reason and the symptom. All 5 added lines present. |
| 7 | `docs/concepts/commands.md`, `docs/concepts/sagas.md` | Fix holds (superseded upward) | The unfiltered-collection-read allowance is present, now inside the broader enumeration row 22 wrote. Superseded, not reverted: the case row 7 covers is still covered, and more strongly. |
| 8 | `docs/concepts/service.md` | Fix holds | § "Custom Repository" now carries the three-file table (`{Aggregate}Repository` / `{Aggregate}CustomRepository` / `{Aggregate}CustomRepositorySagas`) and the profile-agnosticism reason. All 10 added lines present. |
| 9 | `.claude/skills/implement-aggregate/session-b.md` | Fix holds | The "Collection reads have neither path" bullet with its empty-result case is present verbatim. Row's own `Problem` opens `Closes row 7.` |
| 10 | `.claude/skills/implement-aggregate/SKILL.md`, `session-c.md` | Fix holds | The SKILL.md session→file table lists `{Op}CompensationTest.groovy` for session `c`, and `session-c.md` carries the full subsection. All 19 added lines present. |
| 11 | `docs/concepts/testing.md` | Fix holds | § "Soft-delete functionalities - the happy-path state assertion" is present with its Groovy template and the explicit non-extension to the lock-acquisition and compensation cases. All 21 added lines present. |
| 12 | `session-a.md`, `docs/concepts/aggregate.md` | - | Not dismissed: evidences **G1**. |
| 13 | `.claude/skills/implement-aggregate/session-b.md` | Fix holds | The bean template now takes three arguments and the surrounding prose states the constructor/field split. Cross-checked against `service.md` § "Injected Dependencies", whose current example matches exactly. |
| 14 | `docs/concepts/testing.md`, `_shared/conventions.md` | Fix holds | `conventions.md` § "Inspecting maven output" is present with the subprocess recipe, and `testing.md` § Compensation Test points at it. All 24 added lines present. This retrospective used the recipe's sibling procedure for its own build verification. |
| 15 | `.claude/skills/implement-aggregate/session-a.md` | - | Not dismissed: evidences **G2**. |
| 16 | `.claude/skills/implement-aggregate/session-a.md` | Fix holds | The collection bullet now reads "unidirectional and **owning**, mapped through a join table. Do **not** add `mappedBy`…". Present verbatim. |
| 17 | `.claude/skills/implement-aggregate/session-a.md` | - | Not dismissed: evidences **G3**. |
| 18 | `.claude/skills/implement-aggregate/session-b.md` | Fix holds | The "Foreign aggregate ids are never defaulted to a constant" block is present in full. Flagged **Sound but over-broad** in Step 7 for its unforced parameter-overridability prescription, which is a gate observation, not a defect in the fix. |
| 19 | `.claude/skills/implement-aggregate/session-c.md` | Fix holds (superseded upward) | The P4a carve-out is present, in the two-path form row 24 refined it into. The Path A rule row 19 wrote is intact as the first bullet. |
| 20 | `docs/concepts/commands.md` | Already closed | Closed by row 22, whose `Problem` opens `Closes row 20.` The current § "What a Command Is" names the filtered collection read explicitly: "A **filtered** collection read is not one of them: its filter *is* a foreign aggregate id, which it passes." |
| 21 | `docs/concepts/testing.md` | - | Not dismissed: evidences **G4**. |
| 22 | `docs/concepts/commands.md` | Fix holds | The enumeration is now open-ended ("Several step shapes have none"), names the composite-key read, and closes with "All the shapes above declare neither". All 8 added lines present. |
| 23 | `.claude/skills/implement-aggregate/session-a.md` | Fix holds | "**One locked state per mutating saga this aggregate owns**, named `IN_{OPERATION}_{AGGREGATE}`" is present at L142 and survived the later review-lane commit `dce09b3e1`, which removed the adjacent `READ_{AGGREGATE}` bullet without touching it. |
| 24 | `.claude/skills/implement-aggregate/session-c.md` | Fix holds | The carve-out's Path A / Path B split is present verbatim. Row's `Problem` opens `Refines row 19.` |

---

## Type 2 Halts

| # | Session | What the harness did not settle | What the human decided | Was the harness then fixed? |
|---|---------|--------------------------------|------------------------|-----------------------------|
| 2 | 0 | The required JDK was stated nowhere; the shell default was 17 and Step 6's verification build failed with `release version 21 not supported`, a toolchain error that reads like a scaffold bug | Document the requirement in `AGENTS.md` rather than pin it per-directory (e.g. via a committed `.mvn/jvm.config`) | Yes - `395d4ace2`, `AGENTS.md` § "Build Commands" |
| 5 | 1 | A domain enum named by three aggregates: one shared enum or three per-package copies. § "Domain enums" gave only the per-aggregate path | One shared enum at the app source root (`{src}enums/`), imported by every aggregate naming it | Yes - `7d4fad662`, `session-a.md` § "Domain enums" |
| 7 | 2.1.b | An unfiltered collection read has no id to pass as `rootAggregateId`; the `null` allowance was scoped to create commands, and every list-read template took a `{field}Id` | Generalise the `null` rule in `commands.md` rather than add a two-argument `Command` constructor to `simulator/` - the decision that kept this a Type 2 and not a `2-fw` | Yes - `a492262d2` (`commands.md`, `sagas.md`), later broadened by `458f696f3` |
| 9 | 2.1.b | Given row 7's decision, § T2's Path A / Path B not-found rule of thumb still had no branch for a `List`-returning read where an empty result is a valid answer | Resolved under the same decision as row 7: an unfiltered collection read is a first-class shape, and its T2 case is an empty-result case, not a not-found case | Yes - `09cc22b9f`, `session-b.md` |

Rows 12, 15, 17, 20 and 21 are Type 2 but did **not** halt - they are `deferred`, recorded above under
gap **G7**, and four of them are the open gaps G1–G4.

---

## Recorded Limitations

`applications/trainticket/plan.md` carries no "Recorded Limitations" section, so none is inherited.
The confounds below were observed by this retrospective and constrain what it can claim.

- **The whole event-driven lane went unexercised.** `plan.md` § "Application-wide facts" records that
  §4 of the aggregate grouping is empty by decision ("Consistency policy: no cascade"), so no
  aggregate publishes or subscribes, **no aggregate gets a session `d`**, no `EventSubscription`
  subclass was written, no aggregate cached a publisher version, and no rule classified as P2. Phase
  2 was 24 sessions, not 32. Consequently `session-d.md`, `docs/concepts/events.md`, the T3
  subscription test type, and the P2 pattern received **zero** run friction - not because they are
  sound, but because nothing touched them. That this run reports 0 rows against them is evidence of
  nothing. Notably, six of the 22 post-run `/review-artifacts` commits repair exactly that
  untraversed lane (`7440fac9c`, `b06f7859a`, `9f0d85d6d`, `1fef17e67`, `342f77893`, `e60e3196b`),
  including three findings the review rated Critical.
- **The aggregate-boundary checkpoint was skipped for seven of eight boundaries.**
  `docs/workflow.md` § "Aggregate-boundary checkpoint" and `review-artifacts/SKILL.md` both require
  `/review-artifacts` after the last session of every aggregate. `reviews/` contains only two
  reports for this run (`review-2026-08-23.md`, `review-2026-08-23-2.md`), both written after session
  `2.8.c`. Between them they found three Critical defects and produced 22 `harness:` commits -
  guidance that was wrong and uncorrected for the entire run. The convergence curve above therefore
  measures a harness that was *not* being repaired at the rate the workflow prescribes, and a run
  with the checkpoints actually run would very likely show a steeper decline.
- **Single run, single spec pair.** Every claim here is drawn from one application generated from one
  domain-model + aggregate-grouping pair. The convergence pattern is 8 data points; the Type 1
  verdict ratio is 15 edits. Neither is a sample from which a general property of the harness follows.
- **The Type 1 audit is post-hoc and by the same class of agent that made the edits.** Step 7 checks
  each edit against the evidence in its commit and against the artifacts as they stand, which catches
  an edit that contradicts something checkable. It cannot catch an edit that is wrong in a way the
  harness has no other statement about - precisely the Type 2 territory the gate is supposed to route
  to a human.
- **`simulator/` was never touched and never questioned.** Zero `2-fw` rows is a clean result under
  the gate's own terms, but it is also consistent with agents routing around the framework rather
  than around a bug in it. Nothing in this run's evidence distinguishes the two.
