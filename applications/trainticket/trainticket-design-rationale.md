# TrainTicket — Design Rationale

> Companion to [`trainticket-domain-model.md`](trainticket-domain-model.md) and
> [`trainticket-aggregate-grouping.md`](trainticket-aggregate-grouping.md).
>
> This file records **why** the subset is what it is: the decisions taken, the alternatives rejected
> and the reasoning, plus the coverage analysis and threats to validity. The two specification files
> state *what* the application is; this one is the audit trail behind them, written to be cited from
> the dissertation.
>
> Authored 2026-08-13 from a structured design interview. Every factual claim about TrainTicket below
> was verified against the source at `github.com/FudanSELab/train-ticket`; line citations are given
> where a claim is load-bearing.

---

## 1. Why a second application exists

The harness under study was developed against a single reference application, **Quizzes**
(`applications/quizzes-full-2/`). A harness validated on one application cannot be distinguished from
a harness *fitted* to it: every skill, doc and convention had the opportunity to absorb that
application's particular shapes. TrainTicket is introduced as an independent second data point drawn
from a third-party benchmark that neither the harness nor its author influenced.

TrainTicket was chosen because it is the most widely used open-source microservices benchmark in the
software-engineering literature, it is a genuinely different domain (transactional booking rather
than educational assessment), and it is large enough that a subset must be selected — which makes the
selection itself a decision requiring justification rather than an accident.

---

## 2. Method

The subset was fixed through a structured design interview covering eleven decisions, each posed with
its alternatives and their costs before a choice was made. Every decision is recorded in §3 with what
was rejected, because "what was not chosen" is the part that carries the validity argument.

Facts about TrainTicket were established by reading its source rather than its documentation or the
paper's prose, on the grounds that the benchmark's behaviour is what the specification must reflect.
Two of the four findings in §4 were discovered this way and are not documented anywhere upstream.

---

## 3. Decision log

| # | Decision | Chosen | Principal alternative rejected |
|---|---|---|---|
| D1 | Selection criterion | Small runnable subset representing the canonical book-a-ticket flow | Selecting for maximal coverage of shapes Quizzes lacks |
| D2 | Lifecycle scope | Preserve + Pay + Cancel (extended at D10) | Preserve only |
| D3 | Seat availability | Derived by counting the Order aggregate's own rows | A `Departure` aggregate owning a seat map |
| D4 | Aggregate count | All 8 spine aggregates | Trimming Station and/or User |
| D5 | Partitioning | 1:1 with TrainTicket's service boundaries | Co-locating Contacts inside User |
| D6 | Rule invention | Enforced + data-implied tiers, with the benchmark's own defect corrected and documented | Enforced-only; or adding domain-realistic rules |
| D7 | Cascade semantics | None — faithful to the benchmark | Full event cascade with a frozen Order |
| D8 | §3.2 phrasing | Saga-time preconditions; cascade variant recorded as follow-up | Standing invariants over live references |
| D9 | Data-model fidelity | Structure normalised, behaviour faithful | Preserving parallel lists and `String` scalars |
| D10 | Order state machine | 5 states + `refundAmount` | 3 states as originally scoped at D2 |
| D11 | Closing details | Rate limit as constants; name `trainticket`; full query surface incl. `SearchTrips` | A `SecurityConfig` aggregate; minimal reads |

### D1 — Selection criterion

**Chosen:** the smallest runnable subset that faithfully represents TrainTicket's canonical
book-a-ticket flow.

**Rejected:** selecting the subset to maximise coverage of shapes Quizzes lacks. That would have
produced stronger anti-overfitting evidence but is open to the obvious objection that the test case
was chosen with knowledge of where the harness was weak. Shapes Quizzes lacks were still *identified*
(§5), but they were not permitted to drive selection.

**Consequence:** the spine is `ts-preserve-service`'s flow, taken wholesale. No service in that flow
was included or excluded on the grounds of what it would exercise.

### D2 — Lifecycle scope

**Chosen:** booking plus the order lifecycle that follows it.

**Rejected:** booking alone. A booking-only subset yields exactly **one** multi-aggregate saga, with
~15 CRUD operations around it, and leaves `Order.status` written once at creation and never
transitioned. The harness's saga and compensation machinery would each have been exercised once.
Cancellation costs no additional aggregate — `ts-cancel-service` owns no data — and makes the status
field load-bearing.

### D3 — Seat availability

**Chosen:** `OrderService` counts its own active orders for `(tripAggregateId, travelDate,
seatClass)` against a capacity the booking saga supplies from the trip's `TrainType`. Pattern **P3**
for the count, **P4b** for the capacity value.

This mirrors the benchmark exactly: TrainTicket has no seat-inventory entity, and derives
availability via `OrderRepository.findByTravelDateAndTrainNumber` (`OrderRepository.java:26`).
`ts-seat-service` owns no data.

**Rejected:**
- *A `Departure` aggregate owning a seat map*, making capacity and seat uniqueness P1 intra-invariants
  with strong consistency and no TOCTOU window. Cleaner and a genuinely new shape, but it invents a
  ninth aggregate the benchmark does not have.
- *Counters maintained by events*, which would have driven straight into a known unsolved area of the
  harness — `quizzes-full-2-domain-model.md` explicitly defers
  `CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT` with the note that "that counter-based approach has been
  removed; the rule is to be re-implemented later by a different mechanism".
- *No capacity check at all*, which would have removed the only write-contention rule in the
  application.

### D4 — Aggregate count

**Chosen:** all eight data-owning services of the preserve flow become aggregates — Station, Route,
TrainType, PriceConfig, Trip, Contacts, User, Order.

**Rejected:** trimming to 7 or 6 by dropping Station (letting Route hold station names as plain
strings, as TrainTicket half-does) and/or User (folding `accountId` into Contacts). Both were
available and defensible on size grounds. Neither was taken because every one of the eight is
load-bearing in the flow, and five of them are trivial reference data whose implementation weight is
far below the Quizzes aggregates of the same count.

`PriceConfig` was specifically protected despite having only two domain fields: it is the sole entity
in the subset whose identity depends on **two** foreign aggregates, a shape with no Quizzes analogue.

### D5 — Partitioning

**Chosen:** one aggregate per TrainTicket data-owning service.

**Rejected:** co-locating Contacts inside User, or PriceConfig inside Route. Either would have
exercised a shape Quizzes lacks — two independently-identified entities inside one aggregate — but
both depart from the benchmark's own boundaries.

**Validity note:** this makes aggregate partitioning a decision the thesis *inherited* rather than
*made*, which removes a degree of freedom from the experiment. The cost is that the harness's
sensitivity to grouping quality remains untested; see §6.

### D6 — Rule invention

TrainTicket enforces almost nothing. Its entities are anemic and its checks are a handful of
scattered service-level guards. A specification of it must therefore author rules, which is the
sharpest threat to this application's validity.

**Chosen:** a two-tier discipline, recorded in the domain model's preamble.

- **Enforced** — TrainTicket's code checks it somewhere.
- **Implied** — TrainTicket's *data model* requires it for the data to mean anything, but no code
  checks it (e.g. `|stations| == |distances|`, legal status transitions).

**Rejected:** *enforced-only*, which yields ~8 rules dominated by reference-existence checks — the
shape Quizzes already covers in abundance, so the second application would have demonstrated little.
And *adding domain-realistic rules* a railway would need but TrainTicket contains no trace of (no
overlapping trips per route, refund windows, seat interval-packing correctness), which would have
made the difficulty entirely author-controlled.

**No rule in §3 comes from railway domain knowledge that TrainTicket does not itself contain.** That
is the claim the two-tier marking exists to make checkable.

### D7 — Cascade semantics

**Chosen:** none. When reference data changes or is deleted, nothing propagates — reproducing the
benchmark exactly (finding **F2**).

**Rejected:** a full event cascade with Order as a frozen contract, which would have produced ~11
domain events, comparable to Quizzes' 12, and exercised the P2 pattern throughout.

**This is the most consequential decision in the file and the one with the largest cost.** It was
taken deliberately: TrainTicket's dangling-reference behaviour is itself a finding worth reporting,
and reproducing it is more honest than silently repairing it. The costs are stated plainly in §6.

### D8 — §3.2 phrasing

With no events, "`Order.trip` references a Trip that has not been deleted" is unenforceable, because
nothing informs Order. Every §3.2 rule is therefore phrased as a **precondition that held at
operation time**, landing on P4a (saga fetch fails), P3 (service guard over an assembled DTO or the
own table), or P4b (one value passed to the aggregate the saga constructs).

A **cascade variant** — a second aggregate-grouping file over the *same* domain model, adding event
propagation — is recorded as planned follow-up in the grouping file's §5. The template explicitly
permits multiple grouping files per domain model. Two implementations of one domain differing only in
consistency policy would be a sharper experiment than either alone, and it is the only route by which
this domain exercises P2.

### D9 — Data-model fidelity

**Chosen:** normalise the structure, keep the behaviour faithful. Typed scalars, id-plus-cached-name
references, and `Route` owning a `RouteStation` value object.

**Rejected:** preserving TrainTicket's shapes literally. Keeping the two index-correlated lists would
have retained `|stations| == |distances|` as a genuine invariant, but `String`-typed dates make every
temporal rule awkward and break from the simulator's established precedent. Little rule richness was
lost: monotonicity and sequence contiguity survive as P1 invariants.

Reference-by-name was not a choice — the simulator identifies aggregates by `Integer aggregateId`.

### D10 — Order state machine

**Chosen:** `NOTPAID → PAID → COLLECTED → USED`, with `CANCEL` from `{NOTPAID, PAID}`, plus
`refundAmount` computed at cancellation.

This *extends* the D2 scope, on the discovery that `ts-execute-service` owns no data — so
`CollectTicket` and `UseTicket` are single-aggregate status writes costing two §4 rows and no
aggregates. Both transitions and the refund formula are enforced in the benchmark
(`ExecuteServiceImpl`, `CancelServiceImpl.calculateRefund`, `CancelServiceImpl.java:200`).

`refundAmount` was included because it is a **temporal derived value** — its outcome depends on
comparing the cancellation instant against the departure instant — for the cost of one field.

### D11 — Closing details

- **Rate limit as constants.** TrainTicket's scalper check is real and its rule (a count over a
  one-hour window) has no Quizzes analogue, but its thresholds live in a generic `name`/`value`/
  `description` config table. A key-value config table is not domain data, so the rule was kept and
  the ninth aggregate was not.
- **Name `trainticket`.**
- **Full query surface, including `SearchTrips`** — a read spanning five aggregates. In Quizzes only
  one read (`GetQuizAnswerForStudentAndQuiz`) touches a second aggregate at all.

---

## 4. Findings against the benchmark

Four defects and modelling flaws surfaced while writing the specification. F1 and F2 are behavioural
defects; F3 and F4 are modelling flaws. All four are reproduced in the domain model's preamble.

### F1 — the second-class availability check is broken

`PreserveServiceImpl.java:93` gates a second-class booking on:

```java
if (tripResponse.getEconomyClass() == SeatClass.SECONDCLASS.getCode()
        && tripResponse.getConfortClass() == 0) { /* Seat Not Enough */ }
```

- `TripResponse.economyClass` holds the **remaining second-class ticket count**
  (`TravelServiceImpl.java:432`, assigned from `getRestTicketNumber(... SECONDCLASS ...)`).
- `SeatClass.SECONDCLASS.getCode()` is the constant **3** (`SeatClass.java:23`).

The guard therefore fires only when exactly **3** second-class seats remain *and* first class is sold
out. It compares a seat count against an enum code, and conjoins an unrelated class's availability.
**A second-class booking against zero remaining seats is accepted.** The first-class branch, by
contrast, is correct.

`SEAT_CAPACITY_NOT_EXCEEDED` in the domain model is the corrected rule. This is the only place where
the specification knowingly departs from the benchmark's *behaviour* rather than its structure.

### F2 — admin deletion leaves dangling references

`ts-admin-basic-info-service` and its siblings expose `deleteStation`, `deleteTrain`, `deletePrice`,
`deleteRoute`, `deleteTravel` and `deleteContacts`. None propagates. Routes continue to list deleted
stations by name; orders continue to reference deleted trips by `trainNumber`; lowering
`TrainType.economyClass` below the seats already sold for a departure is accepted silently. No service
publishes change events and none subscribes.

This is reproduced faithfully (D7) rather than repaired.

### F3 — `Route` correlates two lists by index

`Route.java:30,34` carries `List<String> stations` and `List<Integer> distances`, related only by
position, with nothing enforcing equal length or consistent ordering. Fare computation
(`BasicServiceImpl.java:102`) indexes both by the same position, so a misalignment silently yields a
wrong fare. Normalised here into an owned `RouteStation` value object.

### F4 — dates and money are `String`

`Order.boughtDate`, `travelDate`, `travelTime` and `price` are all `String`
(`Order.java:32,35,38,71`), parsed ad hoc at each use site. `calculateRefund` reconstructs the
departure instant by parsing two separate strings into `Calendar` objects and passing the extracted
fragments to the deprecated `java.util.Date(int,int,int,int,int,int)` constructor
(`CancelServiceImpl.java:216`). Replaced here with `BigDecimal`, `LocalDate`, `LocalDateTime` and
`LocalTime`.

---

## 5. Shape coverage relative to Quizzes

The point of the second application is the delta. This table is the honest accounting of it.

### Shapes TrainTicket adds

| Shape | Where | Quizzes analogue |
|---|---|---|
| Multi-state lifecycle with an explicit transition table | `ORDER_STATUS_TRANSITION` (5 states) | None — Quizzes uses booleans and date comparisons |
| Capacity constraint by counting own rows on a composite key | `SEAT_CAPACITY_NOT_EXCEEDED` | `UNIQUE_QUIZ_ANSWER_PER_STUDENT` is pair-uniqueness, not a count against a limit |
| Derived cross-aggregate arithmetic asserted as an invariant | `PRICE_MATCHES_TARIFF` (Route distances × PriceConfig rates) | None |
| Entity whose identity depends on two foreign aggregates | `PriceConfig` | None |
| Ordered collection with positional arithmetic | `RouteStation.sequence` + `distanceFromStart` monotonicity | `Option.sequence` exists but carries no arithmetic |
| Count over a temporal window | `ACCOUNT_ORDER_RATE_LIMIT` (orders in the last hour) | None |
| Multi-aggregate read saga | `SearchTrips` (5 aggregates), `GetLeftTicketCount` (3) | Only `GetQuizAnswerForStudentAndQuiz`, touching one |
| An application where P2 is entirely absent | Whole application (D7) | Quizzes is P2-heavy — 12 events |
| Deliberately non-refreshing snapshots as a domain requirement | Order's frozen contract fields | Quizzes refreshes every snapshot it holds |

### Shapes Quizzes covers and TrainTicket does not

| Shape | Consequence |
|---|---|
| P2 / `getEventSubscriptions()` / event handler chains | **Entirely absent.** The whole eventual-consistency mechanism is unexercised |
| Session `d` (`{Aggregate}InterInvariantTest.groovy`, T3 Subscription) | **Vacuous for all 8 aggregates** |
| Event-driven cascade invalidation (`InvalidateQuizEvent`) | Absent |
| Aggregates owning several value-object collections | Quizzes' `Tournament` owns four; TrainTicket's maximum is one (`Route`) |
| N–M collection kept current by events (tournament participants) | Absent |
| Soft-delete cascade rules | Absent — deletion propagates nowhere by design |

### Shared

Soft-delete via the `Aggregate` base class; immutability via Java `final`; own-table uniqueness (P3);
saga-fetch preconditions (P4a); reference-data CRUD; temporal ordering invariants.

### Comparative size

Counted mechanically from the four specification files on 2026-08-13.

| Metric | quizzes-full-2 | trainticket |
|---|---|---|
| Aggregates | 8 | 8 |
| Entities (incl. owned value objects) | 17 | 9 |
| §3.1 single-entity rules | 15 | 24 |
| §3.2 cross-entity rules | 28 (+1 deferred) | 19 |
| §4 functionalities | 46 (27 write, 19 read) | 46 (26 write, 20 read) |
| Multi-aggregate operations | 11 (10 write, 1 read) | 8 (6 write, 2 read) |
| Domain events | 12 | **0** |

The two applications landing on 46 functionalities each is coincidence, not design. The distribution
differs in the way the shape analysis above predicts: TrainTicket carries **60% more single-entity
rules** (24 vs 15) and **fewer cross-entity ones** (19 vs 28), because its complexity sits inside
aggregates — route geometry, status transitions, refund arithmetic — where Quizzes' sits between
them. It also has fewer multi-aggregate writes (6 vs 10) but twice the multi-aggregate reads.

---

## 6. Threats to validity

Stated so the dissertation can address them rather than have them raised for it.

1. **The rules were authored by the same process being evaluated.** TrainTicket enforces almost
   nothing, so any invariant-rich specification of it is partly written rather than transcribed.
   *Mitigation:* the two-tier provenance discipline (D6), with the enforced and implied sets named
   explicitly in the domain model's preamble, and no third tier admitted.

2. **Subset selection could have been tuned to harness strengths.** *Mitigation:* the criterion (D1)
   was fixed to the canonical flow before any service was examined for what it would exercise, the
   spine was taken wholesale, and shapes Quizzes lacks were identified only after selection.

3. **The no-cascade decision removes P2 from the experiment entirely.** This is the largest single
   limitation. A quarter of Phase 2 (session `d`) is vacuous, and the harness's eventual-consistency
   machinery — the most Quizzes-shaped part of it — gets no independent test.
   *Mitigation:* none in this application; the planned cascade variant (D8) exists precisely to
   recover it, and until it is written this limitation stands.

4. **Aggregate boundaries were inherited, not chosen.** A strength for D5's validity, but it means the
   harness's sensitivity to partitioning quality is untested by this application.

5. **One deliberate departure from benchmark behaviour.** `SEAT_CAPACITY_NOT_EXCEEDED` corrects F1
   rather than reproducing it. Implementing a known-defective check would have produced an
   application that cannot satisfy its own capacity rule, but the departure should be declared.

6. **The seat-allocation algorithm is simplified.** TrainTicket's `distributeSeat` reuses a seat whose
   previously sold journey ends at or before the new passenger's boarding station — interval packing
   along the route. This application allocates one seat per journey for the whole trip. Capacity and
   seat uniqueness are therefore stricter here than in the benchmark, which under-counts availability
   relative to TrainTicket but never over-books.

---

## 7. Open items

- **Cascade grouping variant** (D8) — a second aggregate-grouping file over this same domain model,
  adding event propagation. Requires re-reading §3.2, since rules phrased as "held when the operation
  ran" become standing invariants and move from P4a/P3 to P2.
- **Phase 0 onward** — `/boot-strap trainticket`, then `/classify-and-plan` over the two
  specification files, then Phase 2 aggregate by aggregate.
- **Harness finding, deferred by decision.** `classify-and-plan/SKILL.md` step 2.b documents a §3.2
  extraction regex requiring a newline immediately after the rule name; it matches 9 of the 29 rule
  headings in `quizzes-full-2-domain-model.md`. Nothing breaks in practice, since the skill is
  executed by a model rather than a regex engine. Recorded, not fixed.

---
