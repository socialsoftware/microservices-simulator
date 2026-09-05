# TrainTicket — Design Rationale

> Companion to [`trainticket-domain-model.md`](trainticket-domain-model.md) and
> [`trainticket-aggregate-grouping.md`](trainticket-aggregate-grouping.md).
>
> This file records **why** the subset is what it is: the decisions taken, the alternatives rejected
> and the reasoning, plus the coverage analysis and threats to validity. The two specification files
> state *what* the application is; this one is the audit trail behind them, written to be cited from
> the dissertation.
>
> Authored 2026-08-13 from a structured design interview; reviewed and extended 2026-08-22 (§8),
> reviewed again the same day for scope and provenance (§9), and audited a third time before Phase 0
> for provenance reproducibility (§10).
>
> **Source of truth.** Every factual claim about TrainTicket below was verified against
> `github.com/FudanSELab/train-ticket` at commit
> **`313886e99befb94be6cd45f085c98e0019f59829`** (2022-11-01). **All line citations refer to that
> commit** and were re-verified against it on 2026-08-22. The `master` branch has moved since, and its
> line numbers no longer match — a citation checked against `master` will appear wrong when it is not.
> Pinning the commit is what makes this audit trail reproducible.

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
| D11 | Closing details | Rate limit as constants *(reversed at §8 — rule dropped)*; name `trainticket`; full query surface incl. `SearchTrips` *(reversed at §9 — cut, deferred to §7)* | A `SecurityConfig` aggregate; minimal reads |

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

**Allocation policy (settled at the §8 review):** the saga assigns the **lowest seat number in
`[1, capacity]` not already held by a non-cancelled order for that departure**. TrainTicket's
`distributeSeat` picks at random and retries; reproducing that would make every capacity and booking
test non-deterministic, and the simulator offers no seeded-RNG hook. The deterministic rule preserves
the benchmark's intent — one seat per journey, never double-allocated — while remaining assertable.
`SEAT_NUMBER_WITHIN_CAPACITY` records the resulting bound, in the **implied** tier: it derives from
TrainTicket's own seat distribution, not from railway knowledge, so D6's discipline holds.

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
  checks it (e.g. `UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE`, whose repository method returns a
  single row for the pair and so assumes a uniqueness nothing establishes; or
  `DEPARTURE_TIME_MATCHES_TRIP`).

  *This pair of examples replaces two chosen when D6 was written —* `|stations| == |distances|` *and
  "legal status transitions" — both of which the §9 review found are in fact enforced, at
  `RouteServiceImpl.java:38` and in `ExecuteServiceImpl` / `CancelServiceImpl` respectively. That the
  original illustration of the implied tier was drawn from the enforced one is itself a small piece
  of evidence for threat 1.*

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

**`Route.startStationName` / `endStationName` are fidelity, not redundancy.** They duplicate what
`routeStations[0]` and `routeStations[last]` already carry, and `ROUTE_ENDPOINTS_MATCH_STATION_LIST`
exists only to keep the two in step. Both fields are TrainTicket's own — `Route.startStation`
(`Route.java:36`) and `Route.endStation` (`Route.java:38`), stored alongside the `stations` list
rather than derived from it. Reproducing the duplication keeps D6's discipline intact: the invariant
is the **implied** rule that TrainTicket's data model needs for the two representations to agree, not
an authored railway constraint. Dropping the fields would have been the departure requiring
justification, not keeping them. Verified at the §8 review, which is why the citation is recorded
here rather than left implicit.

### D10 — Order state machine

**Chosen:** `NOTPAID → PAID → COLLECTED → USED`, with `CANCEL` from `{NOTPAID, PAID}`, plus
`refundAmount` computed at cancellation.

This *extends* the D2 scope, on the discovery that `ts-execute-service` owns no data — so
`CollectTicket` and `UseTicket` are single-aggregate status writes costing two §4 rows and no
aggregates. Both transitions and the refund formula are enforced in the benchmark
(`ExecuteServiceImpl`, `CancelServiceImpl.calculateRefund`, `CancelServiceImpl.java:200`).

`refundAmount` was included because it is a **temporal derived value** — its outcome depends on
comparing the cancellation instant against the departure instant — for the cost of one field.

**`DeleteOrder` was added at the §8 review.** `OrderRepository` declares `deleteById`, so order
deletion is benchmark behaviour, and without it Order was the only one of the eight aggregates with
no delete operation while two §3.2 rules filtered on a `state != DELETED` condition nothing could
ever make true. Adding it also exposed a latent flaw in `ORDER_REFUND_AMOUNT`, which branched on
`prev.status` while triggering on the standing `CANCELLED` state: soft-deleting a cancelled order
commits a version whose `prev.status` is `CANCELLED`, so the rule fell through to the paid branch and
demanded a refund the order never earned. The rule is now guarded on the cancelling transition.

### D11 — Closing details

- **Rate limit as constants — reversed at the §8 review.** The original decision kept TrainTicket's
  scalper rule (a count over a one-hour window, with no Quizzes analogue) while declining the ninth
  aggregate its thresholds live in, on the grounds that a generic `name`/`value`/`description` config
  table is not domain data. Re-verification found that `ts-security-service` ships **both** thresholds
  as `Integer.MAX_VALUE` (`InitData.java:24,29`), so the check never fires as deployed and any usable
  threshold would have been authored outright. The rule was dropped. See §8 R11 for the reasoning and
  §5 for what the application loses with it.
- **Name `trainticket`.**
- **Full query surface, including `SearchTrips` — reversed at the §9 review.** The original decision
  kept a read spanning five aggregates, on the grounds that in Quizzes only one read
  (`GetQuizAnswerForStudentAndQuiz`) touches a second aggregate at all. It was cut on delivery-cost
  grounds rather than provenance grounds — the first cut in this file made for that reason — and is
  recorded in §7 as the first planned extension once the core is built. Three arguments carried it:

  - It was the only operation in §4 that could not be built in its own session. Trip-primary but
    Order-reading, it had to be deferred to a revisit session after Order's session `c`, and it was
    the sole consumer of the reverse-read protocol added to `classify-and-plan` § "Step 5.5b" and
    `session-b.md` at §8 — new harness machinery with no second user, where a failure would have been
    ambiguous between a harness defect and a specification defect. That ambiguity is expensive in a
    run whose purpose is to measure the harness.
  - The shape it contributed survives. §5's row is "multi-aggregate read saga", and
    `GetLeftTicketCount` still spans three aggregates against Quizzes' one. What is lost is degree,
    five down to three, and §5 records it as such.
  - It is the one cut that D1 arguably *requires*. D1 fixes the criterion as `ts-preserve-service`'s
    flow, taken wholesale. `SearchTrips` is `ts-travel-service.queryForTravels` paired with
    `ts-basic-service` — the **search** flow, not the preserve flow — and the booking saga never
    calls it. Cutting it brings the subset closer to its own stated criterion.

The §8 review had added three operations the query surface needed but did not have:
`GetTripsByRoute` and a widened `GetRoutesByStation` (without them `SearchTrips` had no route from a
station pair to a trip, since `Trip` holds only `routeAggregateId`), and
`GetPriceConfigByRouteAndTrainType` (without which the booking saga had no operation to call for the
fare, even though `UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE` names that exact lookup as the one
whose uniqueness it protects). The first two existed only to serve `SearchTrips` and went with it at
§9; `GetRoutesByStation` reverted to a plain list. `GetPriceConfigByRouteAndTrainType` serves the
booking saga and stays.

---

## 4. Findings against the benchmark

Five defects and modelling flaws surfaced while writing the specification. F1, F2 and F5 are
behavioural defects; F3 and F4 are modelling flaws. All five are recorded in the domain model's
preamble. F1–F4 were found while writing the specification; F5 at the §9 review, while re-testing
the provenance of `PRICE_CONFIG_EXISTS`.

### F1 — the second-class availability check is broken

`PreserveServiceImpl.java:93` gates a second-class booking on:

```java
if (tripResponse.getEconomyClass() == SeatClass.SECONDCLASS.getCode()
        && tripResponse.getConfortClass() == 0) { /* Seat Not Enough */ }
```

- `TripResponse.economyClass` holds the **remaining second-class ticket count**: computed by the
  `getRestTicketNumber(... SECONDCLASS ...)` call at `TravelServiceImpl.java:429-430`, and assigned
  at `TravelServiceImpl.java:432`. *(§8's R9 listed `:432` as re-verified but this section had it as
  the end of the call's argument list and `:434` as the assignment; both were two lines late.
  Corrected at §10.)*
- `SeatClass.SECONDCLASS.getCode()` is the constant **3** (`SeatClass.java:23`).

The guard therefore fires only when exactly **3** second-class seats remain *and* first class is sold
out. It compares a seat count against an enum code, and conjoins an unrelated class's availability.
**A second-class booking against zero remaining seats is accepted.** The first-class branch, by
contrast, is correct.

`SEAT_CAPACITY_NOT_EXCEEDED` in the domain model is the corrected rule. It is one of the two places
where the specification knowingly *corrects* the benchmark's behaviour rather than reproducing it;
the other is F5, found at the §9 review. Until then this section claimed to be the only one. A
correction is not the only way this specification can differ from the benchmark — the *simplification*
recorded as threat 6 also makes it stricter, without claiming the benchmark is wrong — and §10
separates the two vocabularies, which had been used interchangeably.

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

The same class separately stores `startStation` (`Route.java:36`) and `endStation`
(`Route.java:38`) as scalars duplicating the first and last entries of `stations`, again with nothing
keeping them in step — a third representation of the route's shape, and a third way for the three to
disagree. Both fields are retained (D9); `ROUTE_ENDPOINTS_MATCH_STATION_LIST` is the invariant that
closes the gap.

### F4 — dates, money and the seat number are `String`

`Order.boughtDate`, `travelDate`, `travelTime` and `price` are all `String`
(`Order.java:32,35,38,71`), as is `seatNumber` (`Order.java:61`), parsed ad hoc at each use site.
`calculateRefund` reconstructs the departure instant by parsing two separate strings into `Calendar`
objects and passing the extracted fragments to the deprecated
`java.util.Date(int,int,int,int,int,int)` constructor (`CancelServiceImpl.java:216`). Replaced here
with `BigDecimal`, `LocalDate`, `LocalDateTime`, `LocalTime` and `Integer`.

A fifth field is inert rather than mistyped: `coachNumber` (`Order.java:57`) is hard-coded to `5` in
the constructor (`Order.java:79`) and never assigned anywhere in the booking flow, so every order the
benchmark creates is in coach 5. It is dropped here rather than modelled. *(The order-modification
path does copy whatever a caller supplies, at `OrderServiceImpl.java:234` and `:453`. §8's R10
rewrote this sentence to say the field is "never assigned again", which those two lines contradict;
the domain model's own wording, scoped to the booking flow, was already right. Corrected at §10.)*

### F5 — a missing price configuration silently books at an invented fare

`BasicServiceImpl.queryForTravel` computes the fare inside a `try`, and its `catch` is:

```java
}catch (Exception e){
        prices.put("economyClass", "95.0");
        prices.put("confortClass", "120.0");
}
```

`queryPriceConfigByRouteIdAndTrainType` returns `null` when no `PriceConfig` exists for the
`(routeId, trainType)` pair. The multiplication then throws inside the try, the catch swallows it,
and the booking completes at the hard-coded default. `PriceServiceImpl.createNewPriceConfig`
validates neither foreign key, and `AdminBasicInfoServiceImpl.addPrice` is a bare passthrough, so
nothing upstream prevents the pair from being unconfigured either.

Two consequences. No route and train type combination can ever fail for want of a tariff. And
`PRICE_MATCHES_TARIFF` — which the benchmark otherwise genuinely enforces, at
`BasicServiceImpl.java:102` — does not hold on any order the catch produces, because `95.0` is not
`distance × rate` for any distance or rate.

`PRICE_CONFIG_EXISTS` in the domain model is the corrected rule, and it is the **second** of the
specification's two corrections of benchmark behaviour. Like F1, the reason for correcting rather
than reproducing is that reproducing it yields an application whose own booking path can violate its
own rule.

The mechanism is worth stating precisely, because one link in it decides whether the finding holds.
`queryPriceConfigByRouteIdAndTrainType` (`BasicServiceImpl.java:442-455`) hands the price service's
empty response to `JsonUtils.conveterObject`, which serialises `null` to the string `"null"` and
deserialises it back to `null` without throwing (`JsonUtils.java:58-76`). The `null` therefore
reaches line `:106` **inside** the `try` opened at `:100`, and the catch swallows it. Had that
conversion thrown at `:98` instead, the request would have failed outright and there would be no F5.

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
| Multi-aggregate read saga | `GetLeftTicketCount` (3 aggregates) | Only `GetQuizAnswerForStudentAndQuiz`, touching one |
| An application where P2 is entirely absent | Whole application (D7) | Quizzes is P2-heavy — 12 events |
| Deliberately non-refreshing snapshots as a domain requirement | Order's frozen contract fields | Quizzes refreshes every snapshot it holds |

> **One shape was lost at the §8 review.** *Count over a temporal window* —
> `ACCOUNT_ORDER_RATE_LIMIT`, orders in the last hour — was listed here and had no Quizzes analogue.
> It was dropped when re-verification showed the benchmark ships the check disabled (§8 R11). The
> honest accounting is that neither application now exercises a temporal-window count, and that
> TrainTicket's remaining contribution rests on the eight shapes above. Recovering it would mean
> authoring the rule outright, which D6 forbids.

> **One shape was weakened at the §9 review, and none was lost.** The multi-aggregate read row read
> "`SearchTrips` (5 aggregates), `GetLeftTicketCount` (3)" until `SearchTrips` was cut for delivery
> cost (D11). The shape itself survives on `GetLeftTicketCount`, which still spans three aggregates
> against Quizzes' one, so the row is a degradation from five to three rather than a deletion. This is
> a weaker claim than the one originally made and is recorded as such. Unlike the §8 losses it is
> recoverable without authoring anything: the operation is fully specified in this file's §7 and can
> be restored once the core application is delivered.

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

Counted mechanically from the four specification files, recounted on 2026-08-22 after the scope and
provenance review in §9, and recounted again at the §10 audit, which changed none of them. The trainticket column fell from 20 / 49 / 8 at §8: four §3.2 rules were
dropped for failing the provenance test or duplicating another rule, and two read operations were cut
with `SearchTrips`. Three of the four dropped rules were the only reason their creating operation
touched a second aggregate, which is why the multi-aggregate count falls further than the rule count
alone would suggest.

| Metric | quizzes-full-2 | trainticket |
|---|---|---|
| Aggregates | 8 | 8 |
| Entities (incl. owned value objects) | 17 | 9 |
| §3.1 single-entity rules | 15 | 23 |
| §3.2 cross-entity rules | 28 (+1 deferred) | 16 |
| §4 functionalities | 46 (27 write, 19 read) | 47 (27 write, 20 read) |
| Multi-aggregate operations | 11 (10 write, 1 read) | 5 (4 write, 1 read) |
| Domain events | 12 | **0** |

The two applications are within one functionality of each other, which is coincidence rather than
design. The distribution differs in the way the shape analysis above predicts, and after §9 it does
so more sharply: TrainTicket carries **over half again as many single-entity rules** (23 vs 15) and
**little more than half as many cross-entity ones** (16 vs 28), because its complexity sits inside
aggregates — route geometry, status transitions, refund arithmetic — where Quizzes' sits between
them. The gap in multi-aggregate operations (5 vs 11) is now the starkest number in the table, and it
is the honest consequence of two decisions taken for different reasons: D7 removed the events, and §9
removed four cross-aggregate rules that had no benchmark support to justify the fetches they implied.
An equal-sized specification with less than half the cross-aggregate coordination is the shape this
application actually has.

---

## 6. Threats to validity

Stated so the dissertation can address them rather than have them raised for it.

1. **The rules were authored by the same process being evaluated.** TrainTicket enforces almost
   nothing, so any invariant-rich specification of it is partly written rather than transcribed.
   *Mitigation:* the two-tier provenance discipline (D6), with the enforced and implied sets named
   explicitly in the domain model's preamble, and no third tier admitted.

   *This threat is the one that has cost the most, and the accounting should be stated plainly.* The
   §8 review applied the test to two candidate rules and **removed both** — a threshold the benchmark
   ships disabled, and a format nothing validates (R11, R12). The §9 review then applied it to every
   rule already admitted, which §8 had not done, and found **seven of the twenty §3.2 provenance
   assignments wrong**: four rules claimed enforced had no enforcement site anywhere
   (`ACCOUNT_EXISTS` in both blocks, `ROUTE_AND_TRAIN_TYPE_EXIST` in its PriceConfig block, and
   `SEAT_CLASS_OFFERED`), and three claimed implied were in fact enforced (`UNIQUE_STATION_NAME`,
   `UNIQUE_TRIP_NUMBER`, `UNIQUE_USER_NAME`). §3.1 contributed an eighth,
   `ORDER_SEAT_NUMBER_POSITIVE`, and D6's own illustration of the implied tier turned out to be drawn
   from the enforced one.

   Two readings follow and both should be kept. The errors ran in **both** directions, three of the
   eight understating the benchmark's support and five overstating it, which is what an
   author-in-good-faith error distribution looks like rather than a systematic bias toward invention.
   But the four overstatements were all of one kind — a reference-existence check assumed to be
   somewhere in a forty-service codebase, because such a check is what a careful author would write
   — and they survived one full review that had already been told to look for exactly this. **A
   provenance tier is only as good as the pass that last re-derived it**, and neither §8 nor the
   original authoring pass re-derived it; §8 re-verified line numbers, which is a weaker test than it
   appears, because a citation can point at the right line for a claim that line does not support.
   The four rules were dropped rather than re-tiered, so the specification is now smaller than the
   threat's mitigation would strictly require. That is deliberate: under D6 cutting is always
   available and inventing never is, and a rule that has already been mis-tiered once has earned no
   presumption.

   *§10 then found that §9 had a residual blind spot of its own shape.* §9 re-derived every §3.2
   rule against the source, which is the right test, but it applied that test to rules whose cited
   site is a single guard — a call that returns, a repository lookup that rejects. It did not ask
   whether a cited site that is a **loop, a branch or a partial guard** establishes the rule's
   *stated* predicate or only part of it. Two rules failed on exactly that. The seat-uniqueness retry
   loop is skipped entirely on the benchmark's segment-reuse path, so it never establishes the
   unconditional uniqueness the rule asserted; and the order status guards cover every transition
   except the one into `PAID`, which nothing guards. Both are now declared honestly — one re-tiered,
   one split across tiers. The lesson generalises past this application: **re-deriving a tier means
   re-deriving the whole predicate, not confirming that the cited code is about the right subject.**
   A site can be about exactly the right subject and still permit what the rule forbids. That is a
   third distinct way for a provenance claim to fail, after "no site at all" (§9) and "wrong line"
   (§8), and it is the one that survives both of the earlier tests.

2. **Subset selection could have been tuned to harness strengths.** *Mitigation:* the criterion (D1)
   was fixed to the canonical flow before any service was examined for what it would exercise, the
   spine was taken wholesale, and shapes Quizzes lacks were identified only after selection.

3. **The no-cascade decision removes P2 from the experiment entirely.** This is the largest single
   limitation. Because no aggregate has subscribed events, session `d` is never generated at all —
   Phase 2 is 24 sessions (8 × `a b c`), not 32 with eight empty ones. The T3 Subscription test type
   and the harness's whole eventual-consistency machinery — the most Quizzes-shaped part of it — get
   no independent test here.
   *Mitigation:* none in this application; the planned cascade variant (D8) exists precisely to
   recover it, and until it is written this limitation stands.

4. **Aggregate boundaries were inherited, not chosen.** A strength for D5's validity, but it means the
   harness's sensitivity to partitioning quality is untested by this application.

5. **Two deliberate corrections of benchmark behaviour.** `SEAT_CAPACITY_NOT_EXCEEDED` corrects F1
   rather than reproducing it, and `PRICE_CONFIG_EXISTS` corrects F5. In both cases implementing the
   benchmark faithfully would have produced an application whose own booking path violates its own
   rule — an over-booking past capacity in the first case, an order priced at neither
   `distance × rate` nor anything else derivable in the second. Both are corrections of defects,
   never additions of constraints, and both are declared in the domain model's preamble. The second
   was found at §9; until then this threat claimed there was only one, which was itself a small
   instance of threat 1.

   *This threat counts corrections only, and §10 tightened the wording to say so.* It previously read
   "departures from benchmark behaviour", which is a wider category that also covers the
   simplifications in threat 6 — and by that wider reading the count of two was wrong, since
   `SEAT_NUMBER_UNIQUE_PER_DEPARTURE` forbids an allocation the benchmark performs on purpose. The
   two threats partition the difference between this application and the benchmark: **a correction
   says the benchmark is wrong and departs from it; a simplification says the benchmark is more
   elaborate than this study needs and is stricter than it.** Corrections are the ones that need
   defending, because only they assert something about the benchmark. There are two, and the count is
   now stable under the narrower definition it was always meant to have.

6. **The seat-allocation algorithm is simplified.** TrainTicket's `distributeSeat` reuses a seat whose
   previously sold journey ends at or before the new passenger's boarding station
   (`SeatServiceImpl.java:100-108`) — interval packing along the route. This application allocates one
   seat per journey for the whole trip. Capacity and seat uniqueness are therefore stricter here than
   in the benchmark, which under-counts availability relative to TrainTicket but never over-books. It
   is also **deterministic** where the benchmark is random (D3): the lowest free seat number rather
   than a retry loop. This makes the tests assertable at the cost of one more divergence, and it
   changes which seat a passenger gets but never how many passengers fit.

   *This threat is what carries `SEAT_NUMBER_UNIQUE_PER_DEPARTURE`, and §10 made that load-bearing
   rather than incidental.* The rule was in the **enforced** tier until then, citing the
   `isContained` retry loop. But that loop sits *after* the reuse branch and is skipped whenever
   reuse applies, so the benchmark deliberately issues two non-cancelled orders on one departure
   holding the same seat number: the rule's unconditional predicate is false in the benchmark, and
   the cited site is the right code for a claim it does not support. The rule survives only because
   this threat removes interval packing, and once packing is gone the retry loop *is* the allocator
   and uniqueness follows. So it is now **implied** — a consequence of a declared simplification, not
   a transcribed check — alongside `ORDER_SEAT_NUMBER_POSITIVE` and `SEAT_NUMBER_WITHIN_CAPACITY`.
   All three seat rules now sit in one tier as three consequences of one allocator, which is the
   arrangement §9 reached for the other two and stopped one rule short of.

---

## 7. Open items

- **Cascade grouping variant** (D8) — a second aggregate-grouping file over this same domain model,
  adding event propagation. Requires re-reading §3.2, since rules phrased as "held when the operation
  ran" become standing invariants and move from P4a/P3 to P2.
- **Phase 0 onward** — `/boot-strap trainticket`, then `/classify-and-plan` over the two
  specification files, then Phase 2 aggregate by aggregate: 24 sessions, 8 × `a b c`, with no session
  `d` (empty §4) and, after the §9 cuts, no revisit session either.
- **Harness finding, deferred by decision.** `classify-and-plan/SKILL.md` step 2.b documents a §3.2
  extraction regex requiring a newline immediately after the rule name; it matches 9 of the 29 rule
  headings in `quizzes-full-2-domain-model.md`. Nothing breaks in practice, since the skill is
  executed by a model rather than a regex engine. Recorded, not fixed.
- **`SearchTrips`, the first planned extension after the core is delivered.** Cut at §9 for delivery
  cost, not for provenance: it is benchmark behaviour
  (`ts-travel-service.queryForTravels` paired with `ts-basic-service`) and remains fully specified
  here, so restoring it authors nothing. Restoring it means: re-adding the `SearchTrips` row to §4
  (Trip-primary; Route, TrainType, PriceConfig, Order); re-adding `GetTripsByRoute`; widening
  `GetRoutesByStation` to return each route in full including its ordered `routeStations`, so a
  caller can intersect two stations and check their relative sequence; and restoring §5's
  multi-aggregate read row to five aggregates. The search resolves a station pair by running
  `GetRoutesByStation` for the departure and arrival stations, intersecting the two result sets,
  keeping the routes whose `routeStations` place the departure before the arrival, then calling
  `GetTripsByRoute` for each survivor; fares come from `GetPriceConfigByRouteAndTrainType` and
  remaining seats from Order. **It is Trip-primary but reads Order, which the topological sort places
  last**, so it cannot be built in Trip's session `b` and needs a revisit session after Order's
  session `c`, once `OrderDto` exists — the reverse-read case that `classify-and-plan` § "Step 5.5b"
  and `session-b.md` were extended to catch at §8. That machinery is in place and unexercised; this
  extension is what would exercise it.
- **Rule-name collisions in the §3.2 parser, recorded not fixed — and now latent here.**
  `classify-and-plan` step 4 keys its classification map by the rule name its regex captures
  (`[A-Z_0-9]+`), which drops the parenthesised qualifier. This file had two collisions until §9:
  `ROUTE_AND_TRAIN_TYPE_EXIST (Trip)` against `(PriceConfig)`, and the two `ACCOUNT_EXISTS` blocks.
  Dropping three of those four rules removed both, so **no §3.2 rule name in this application now
  collides** — but by accident, not by fix. `quizzes-full-2` still has the shape, with three
  `COURSE_EXECUTION_EXISTS` blocks, and Phase 1 handles it correctly because the skill is executed by
  a model rather than a regex engine. Left alone for the same reason as the item above.

---

## 8. Design review — 2026-08-22

The two specification files were reviewed for internal consistency and implementability before Phase
0. Twelve changes followed, two of them reversing earlier decisions. They are recorded here rather
than folded into §3 so that the decision log stays a record of what was decided **when**: §3 is the
original design interview, this section is what a second pass found in it.

The two reversals (R11, R12) both went the same way, and both cost the application something: a rule
was removed because re-reading the source showed it had less benchmark support than §3 had assumed.
That direction is worth noting, because it is the direction threat 1 predicts — a specification of a
benchmark that enforces almost nothing will drift toward authoring, and the drift is visible only when
the claims are checked one at a time against the source.

Every line citation in this file was re-checked against the pinned commit during the review:
`Route.java` 30/34/36/38, `OrderRepository.java:26`, `PreserveServiceImpl.java:93`,
`SeatClass.java:23`, `TravelServiceImpl.java:432`, `CancelServiceImpl.java:200` and `:216`,
`BasicServiceImpl.java:102`, and `Order.java` 32/35/38/57/61/71/79. All were correct as cited; the
two errors found (R10) were in prose, not in line numbers.

| # | Finding | Resolution |
|---|---|---|
| R1 | `SearchTrips` is Trip-primary but reads Order, which the topological sort places last; it cannot be built in its own session `b`, and `classify-and-plan` § "Step 5.5" detects reverse dependencies only for write guards | Deferred to a revisit session after Order's `c`, marked `⚠️` in §4; harness extended (Step 5.5b, `session-b.md`) |
| R2 | `SearchTrips` had no path from a station pair to a trip — `Trip` holds only `routeAggregateId` | §4 gains `GetTripsByRoute`; `GetRoutesByStation` widened to return the full route including `routeStations` |
| R3 | The booking saga's fare lookup had no operation to call, though `UNIQUE_PRICE_CONFIG_PER_ROUTE_AND_TRAIN_TYPE` names that exact lookup | §4 gains `GetPriceConfigByRouteAndTrainType`; §3.2 gains `PRICE_CONFIG_EXISTS` (implied tier, P4a) |
| R4 | Nothing said where `Order.seatNumber` came from, and nothing bounded it above | Deterministic lowest-free-in-`[1, capacity]` allocation stated in §4; §3.2 gains `SEAT_NUMBER_WITHIN_CAPACITY` (implied tier) |
| R5 | `SEAT_CAPACITY_NOT_EXCEEDED` was written as a post-state invariant but enforced as a pre-mutation P3 guard — transcribed literally, it admitted `capacity + 1` bookings | Restated over *existing* rows with strict `<`, matching the §3.2 preamble's own precondition framing |
| R6 | Two seat rules filtered on `state != DELETED`, which no operation could make true; adding the missing delete exposed `ORDER_REFUND_AMOUNT` branching on `prev.status` while triggering on the standing `CANCELLED` state | §4 gains `DeleteOrder`; `ORDER_REFUND_AMOUNT` guarded on the cancelling transition; `ORDER_STATUS_TRANSITION` pins `prev == null ⟹ NOTPAID` |
| R7 | `Order.accountAggregateId` and `Contacts.userAggregateId` named the same target two ways | Unified on `userAggregateId` |
| R8 | `Route.startStationName` / `endStationName` carried no citation, leaving the rule most exposed to threat 1 unsupported | Verified present in the benchmark (`Route.java:36,38`); kept, citation recorded in D9 and F3 |
| R9 | Line citations were unreproducible against a moving `master` | Commit `313886e9` pinned in the preamble; all citations re-verified against it |
| R10 | Re-verification found two wrong claims of its own: F4 omitted `Order.seatNumber` from the `String`-typed fields, and the preamble said `coachNumber` "is never assigned" when the constructor hard-codes it to `5` | F4 extended to five fields; the `coachNumber` note corrected — an inert constant is a better reason to drop the field than an unassigned one |
| R11 | `ACCOUNT_ORDER_RATE_LIMIT` failed the provenance test: `ts-security-service` ships both thresholds as `Integer.MAX_VALUE` (`InitData.java:24,29`), so the check never fires as deployed and any usable value would have been authored (D11 had assumed the config rows carried real numbers) | Rule **dropped**. §3.2 falls to 20. Costs the "count over a temporal window" shape in §5 — recorded there as a loss rather than quietly deleted |
| R12 | `TRIP_NUMBER_FORMAT` was listed in the **enforced** tier, but nothing validates a trip number; `SeatServiceImpl` and `AdminOrderServiceImpl` only branch on the leading letter to select high-speed behaviour, accepting anything else | Rule **dropped** rather than re-tiered. §3.1 falls to 23. A format no code enforces and no other rule depends on carries no weight |

Two claims were also corrected. Grouping §5 and threat 3 said session `d` was "vacuous for all eight
aggregates" and cost "a quarter of Phase 2"; in fact the harness omits the session entirely when an
aggregate subscribes to no events, so Phase 2 is 24 sessions rather than 32 and the real cost is that
the T3 test type is unexercised. The §5 size table was recounted.

**Harness repairs made in the same pass** (`AGENTS.md` § "Harness evolution"): three Type 1 fixes to
`classify-and-plan` — version fields made conditional on event subscription, `× N` accepted in either
§2 column, and collection-snapshot class naming corrected for rows that name the owned entity
explicitly — plus one Type 2 extension authorised by the reviewer, the reverse-**read** detection in
Step 5.5b and its counterpart protocol in `session-b.md`. None of the three Type 1 defects was the
specification's fault; in all three the specification was on the correct side and the skill was wrong.

---

## 9. Scope and provenance review — 2026-08-22

A second review the same day as §8, with a different question. §8 asked whether the two specification
files were internally consistent and implementable. This one asked two things §8 did not: whether
every rule already admitted still passes D6's provenance test when re-derived from the source, and
what the application would lose if the expensive parts of it were cut to shorten first delivery.

It is recorded separately from §8 for the same reason §8 is recorded separately from §3: each section
is what a given pass found, and collapsing them would hide that §8's pass did not catch what §9's
did.

**Why §8 missed the provenance errors.** §8's R9 re-verified every line citation against the pinned
commit and found them all correct, which is a weaker guarantee than it looks: a citation can point at
exactly the right line for a claim that line does not support. R11 and R12 applied the provenance
test properly, but only to two *candidate* rules — ones already under suspicion. No pass had ever
re-derived the tier of a rule that was already admitted and unremarkable. The four enforced-tier
errors below all sat in that blind spot.

### Provenance corrections

Every §3.2 rule was re-derived from the source at commit `313886e9`. **Seven of the twenty
assignments were wrong**, plus one in §3.1.

Claimed enforced, no enforcement site — all four **dropped** rather than re-tiered, since under D6
cutting is always available and a rule mis-tiered once has earned no presumption:

| Rule | What the source actually does |
|---|---|
| `ACCOUNT_EXISTS` (Contacts) | `ts-contacts-service` never calls `ts-user-service` at all; `create` checks only for a duplicate document on the account |
| `ACCOUNT_EXISTS` (Order) | `PreserveServiceImpl` fetches the account once, at `:245`, **after** `createOrder` has succeeded at `:170`, and never rejects on it — the result only fills a `NotifyInfo` whose send call at `:261` is commented out. *(This row said the fetch was inside `sendEmail`; that method, at `:288`, is dead code. Corrected at §10; the drop stands.)* |
| `ROUTE_AND_TRAIN_TYPE_EXIST` (PriceConfig) | `AdminBasicInfoServiceImpl.addPrice` is a passthrough; `createNewPriceConfig` validates neither foreign key. The **Trip** block of the same rule is genuinely enforced by `AdminTravelServiceImpl.checkTravelInfo` and is kept |
| `SEAT_CLASS_OFFERED` | Nothing tests capacity for zero. The first-class guard reads the *remaining* count, so the benchmark cannot tell a class that does not exist from one that is sold out — that guard is `SEAT_CAPACITY_NOT_EXCEEDED` |

Claimed implied, actually enforced — all three **re-tiered**, none dropped:

| Rule | Enforcement site |
|---|---|
| `UNIQUE_STATION_NAME` | `StationServiceImpl.create` rejects a duplicate name; `Station.name` is also `@Column(unique = true)` |
| `UNIQUE_TRIP_NUMBER` | `TravelServiceImpl.java:61` declines to save a second trip under an existing id. It returns status `1` with the message "already exists" rather than an error, so "rejects" overstates the response, but no duplicate is written and the uniqueness is genuinely code-maintained |
| `UNIQUE_USER_NAME` | `UserServiceImpl.saveUser`, whose own comment reads `// avoid same user name` |

Two further corrections:

- **`ORDER_SEAT_NUMBER_POSITIVE` re-tiered to implied.** It and `SEAT_NUMBER_WITHIN_CAPACITY` are the
  two halves of one expression, `rand.nextInt(range) + 1` in `SeatServiceImpl.distributeSeat`.
  Nothing *checks* a seat number; the allocator *produces* one. Splitting them across tiers was
  arbitrary, and the split had been load-bearing: it was the stated reason for keeping
  `SEAT_CLASS_OFFERED` alongside the rule that already subsumed it.
- **`ROUTE_ENDPOINTS_MATCH_STATION_LIST` straddles both tiers.**
  `AdminRouteServiceImpl.java:58` enforces *containment* of the declared endpoints in the station
  list; nothing requires them to be the **first and last** entries. The positional strengthening is
  kept — `Route.stations` is `@OrderColumn` and `startStation` means origin, so the data model does
  need it — but it is now declared as implied rather than passed off as part of an enforced check.
  *(This bullet called it the only rule that straddles. §10 found a second,
  `ORDER_STATUS_TRANSITION`, whose edge into `PAID` no code guards.)*
- **D6's own example of the implied tier was drawn from the enforced one.** It offered
  `|stations| == |distances|` and "legal status transitions"; the first is checked at
  `RouteServiceImpl.java:38`, the second in `ExecuteServiceImpl` and `CancelServiceImpl`. Replaced.

### F5

Re-testing `PRICE_CONFIG_EXISTS` surfaced a fifth finding against the benchmark, of the same class as
F1: `BasicServiceImpl.queryForTravel` wraps the fare computation in a `try` whose `catch` writes a
hard-coded `95.0` / `120.0`, so a missing price configuration books at an invented fare instead of
failing. §4 records it. It makes `PRICE_CONFIG_EXISTS` the **second** of the specification's
departures from benchmark behaviour, where the preamble and threat 5 had both claimed there was only
one.

### Scope cuts

The brief was a subset large enough to be interesting and small enough to deliver quickly, cutting
what adds implementation complexity without being essential to the core booking application. Each cut
was put separately with its knock-on effects. Cuts made:

| Cut | Reason | Effect |
|---|---|---|
| `SearchTrips`, `GetTripsByRoute`; `GetRoutesByStation` un-widened | Delivery cost (D11, reversed) | §4 49 → 47; removes the one out-of-band revisit session; §5's multi-aggregate read degrades 5 → 3 |
| `SEAT_CLASS_OFFERED` | No enforcement site; duplicates `SEAT_NUMBER_WITHIN_CAPACITY` | §3.2 20 → 19 |
| `ACCOUNT_EXISTS` (Contacts) | No enforcement site | `CreateContacts` becomes a single-aggregate write |
| `ACCOUNT_EXISTS` (Order) | No enforcement site | `PreserveTicket` stops reading User: 6 other aggregates → 5 |
| `ROUTE_AND_TRAIN_TYPE_EXIST` (PriceConfig) | No enforcement site | `CreatePriceConfig` becomes a single-aggregate write; §3.2 → 16 |

Note that only the first was a scope decision. The other four are provenance corrections that happen
to reduce scope, which is why the multi-aggregate count falls further than a purely cost-driven trim
would have taken it.

Cuts **considered and rejected**, since what was declined carries as much of the argument as what was
taken:

- **Dropping the `Station` aggregate**, letting `RouteStation` hold a plain station name as
  TrainTicket's `Route` literally does (`List<String> stations`). Available, and closer to the
  benchmark's data model than the current normalisation. Rejected because it breaks D5's 1:1
  aggregate-to-service mapping — a stated validity claim — to save the three *lightest* sessions in a
  24-session run. The lever with the largest denominator is only ever available on the cheapest
  aggregates, which is what makes it a bad trade.
- **Dropping `User`**, the other trim D4 considered. Rejected: it removes more rules than Station but
  takes `GetOrdersByAccount`, `GetContactsByAccount` and the whole notion of who booked a ticket with
  it, which is a larger hole in a booking application.
- **`CollectTicket` and `UseTicket`**, cutting the lifecycle to three states. Rejected as the worst
  ratio available: two single-aggregate status writes with no saga and no new fetch, against §5's
  leading shape, the five-state transition table with no Quizzes analogue.
- **The eight `Delete*` operations.** Rejected: they are the cheapest rows in §4 and they carry F2
  and D7, the file's central finding. With nothing deleted, nothing dangles, and the no-cascade
  decision would have had nothing to demonstrate. *(This bullet said seven. §4 declares eight:
  `DeleteStation`, `DeleteRoute`, `DeleteTrainType`, `DeletePriceConfig`, `DeleteTrip`, `DeleteUser`,
  `DeleteContacts` and `DeleteOrder`. Corrected at §10.)*
- **`PriceConfig`, folded into `Route`.** Rejected: D4 protected it as the only entity whose identity
  depends on two foreign aggregates, a §5 shape with no Quizzes analogue, and folding it would remove
  that shape outright as well as breaking D5.
- **`Route.startStationName` / `endStationName`.** Rejected: one P1 rule saved against F3's third
  representation of the route's shape losing its illustration.

### Resulting size

8 aggregates, 9 entities, 23 §3.1 rules, 16 §3.2 rules, 47 functionalities (27 write, 20 read),
5 multi-aggregate operations (4 write, 1 read), 0 domain events. Phase 2 is 24 sessions — 8 × `a b c`,
no session `d`, and now no revisit session.

D5 is intact: all eight data-owning services of the preserve flow remain aggregates, and no cut
touched the partitioning.

---

## 10. Provenance reproducibility audit — 2026-08-22

A third pass, immediately before Phase 0, with a narrower question than either of the first two. §8
asked whether the two specification files are internally consistent and implementable. §9 asked
whether every admitted rule still passes D6's provenance test. This one asked whether a reader with
only the clone and these files can **reproduce each enforced-tier assignment from the source**, and
treated a correct citation as insufficient evidence on its own.

It is recorded separately for the same reason §9 is recorded separately from §8: each section is what
a given pass found, and collapsing them would hide that §9's pass did not catch what this one did.

**Why §9 missed these.** §9 re-derived every §3.2 rule against the source, which is the right test,
but every rule it re-derived had a cited site that is a *single guard* — a call that returns, a
repository lookup that rejects. Such a site either exists or it does not, which is what made §9's
sweep tractable and what made its four "no enforcement site" findings clean. It never asked whether a
site that is a **loop, a branch, or a guard covering some transitions and not others** establishes
the rule's whole stated predicate. Both findings below sat in that gap. This is a third distinct way
for a provenance claim to fail, after "wrong line" (§8's concern) and "no site at all" (§9's), and it
is the only one that survives both earlier tests — see threat 1.

### Provenance corrections

Every §3.2 and §3.1 enforced-tier assignment was re-derived at commit `313886e9`, this time by
checking the *predicate* against the site rather than the *subject*. Two of the thirteen were wrong.
Both were kept in some form; neither was dropped, because in each case the rule states something the
application genuinely needs and the defect was in the tier, not the rule.

| Rule | What the source actually does | Resolution |
|---|---|---|
| `SEAT_NUMBER_UNIQUE_PER_DEPARTURE` | The cited `isContained` retry loop (`SeatServiceImpl.java:109-111`) is never reached on the segment-reuse path. At `:100-108`, before any uniqueness check, `distributeSeat` returns `soldTicket.getSeatNo()` for any sold ticket whose destination precedes the new boarding station, so the benchmark issues duplicate `(trip, date, class, seat)` tuples **on purpose** | **Re-tiered to implied.** The rule holds only under threat 6's removal of interval packing, which is a simplification this file already declared. It joins `ORDER_SEAT_NUMBER_POSITIVE` and `SEAT_NUMBER_WITHIN_CAPACITY`: all three seat rules are now consequences of one allocator, in one tier |
| `ORDER_STATUS_TRANSITION` | The three cited guards are real and cover every edge into `COLLECTED`, `USED` and `CANCELLED`. Nothing guards the edge into `PAID`: `OrderServiceImpl.payOrder` writes `status = PAID` without reading the previous status, so a `CANCELLED` or `USED` order can be paid | **Declared as straddling both tiers**, like `ROUTE_ENDPOINTS_MATCH_STATION_LIST`. The guarded edges stay enforced; the `→ PAID` restriction is implied. §9's claim that `ROUTE_ENDPOINTS_MATCH_STATION_LIST` was the only straddling rule is withdrawn |

Both were verified in the other direction too: the eleven remaining enforced assignments and all five
implied §3.2 assignments reproduce as stated. `TRIP_EXISTS` and `CONTACTS_EXIST` had been the only
enforced rules with no named site, contrary to the preamble's own promise that every assignment names
one; both reproduce, and both now carry their sites.

### Specification corrections

- **Two §3.2 Entities cells named an aggregate no declared operation reaches.**
  `ENDPOINTS_ON_TRIP_ROUTE` listed Station and `CONTACTS_BELONG_TO_ACCOUNT` listed User, but both
  predicates resolve entirely from data §4's declared fetches already carry, and §4 is the complete
  inventory. `classify-and-plan` step 6.c selects cross-aggregate prerequisites by entity membership,
  so both would have produced a saga fetch with no operation behind it. Both cells trimmed, each with
  a note saying why the aggregate is absent. Quizzes' comparable blocks name only what the operation
  fetches, so this was a divergence from the reference application, not a convention.
- **`SearchTrips` and the other §9 cuts** were re-checked for live references across all three files.
  None remains: every mention is in a drop list, a cut explanation, or §7's planned-extension text.

### Corrections to earlier sections

Five factual errors in §8's and §9's own prose, none of which changes a decision:

| Where | Error | Correction |
|---|---|---|
| §9, `ACCOUNT_EXISTS` (Order) | The account fetch is "inside `sendEmail`" | It is at `PreserveServiceImpl.java:245`, in `preserve()` itself. `sendEmail` (`:288`) is dead code — its only call site, `:261`, is commented out. The drop stands on the timing, which was right |
| F1 | The `SECONDCLASS` call's argument list "ends at `:432`", assigned at `:434` | The call is `:429-430` and the assignment `:432`. Both were two lines late, and `:432` is one of the citations §8's R9 reported as re-verified |
| F4 | `coachNumber` is "never assigned again" | `OrderServiceImpl.java:234` and `:453` assign it on the order-modification path. The domain model's wording, scoped to the booking flow, was already correct; §8's R10 introduced the unqualified version |
| §9, `UNIQUE_TRIP_NUMBER` | `TravelServiceImpl.java:61` "rejects" an existing trip id | It declines the save but returns status `1`. The uniqueness is still code-maintained, so the enforced tier is unaffected |
| §9, cuts rejected | "The seven `Delete*` operations" | §4 declares eight |

`PRICE_MATCHES_TARIFF`'s citation was widened from `BasicServiceImpl.java:102` to `:102-107`: `:102`
is the distance subtraction, and the multiplication the rule actually asserts is at `:106` and `:107`.
The tier was never in doubt; the citation named half the expression.

### What did not change

The size table is untouched: 8 aggregates, 9 entities, 23 / 16 / 47 (27 write, 20 read), 5
multi-aggregate (4 write, 1 read), 0 events, 24 sessions with no session `d` and no revisit session.
All eight counts were recounted mechanically from the files rather than carried forward. §5's
shape-coverage table survives unchanged — no shape rested on either mis-tiered rule — and **D5 is
intact**, since nothing here touches the partitioning. The corrections were to tiers, citations and
prose; the application this specification describes is the same one §9 left behind.

---
