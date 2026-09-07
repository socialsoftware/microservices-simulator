# Event-order-dependent rejection experiment

## Approved direct brief

Compare two versions of the same workload, moving only one selected event attempt
across a target operation step. Qualify a Tournament name-propagation candidate and a
removed-enrolment negative control. Keep observations separate from ImpactV2. Use the
existing prepared executor, source-derived setups, retained packages and reports; no
Quizzes or verifier production changes, score expansion, automatic search or meeting-note
rewrite. The user approved this bounded experiment and a small reusable comparison on
8 September 2026. Work in the current checkout and retain results in a local commit.

## Fixed design

Four existing workload/scenario records are selected structurally before new outcomes.
Within each pair, the declarations, inputs, setup ID, source steps, event routes and
nonmoved action order are identical. Only e1 moves; it remains after its producer. All
fault vectors are zero. The Tournament pair retains the extra CourseExecution read in
both versions, so this does not compare the earlier w03 and w05 as if event order were
their only difference. Its unchanged QuizAnswer attempt remains after AddParticipant.
The negative pair preserves removal before name update in both versions and moves only
the first selected removal-event attempt across that update.

Each version runs twice, in fresh Docker/JVM/H2 instances, serially. The second pass
reverses launch order. Dates use the same relative setup recipes; they are not identical
wall-clock timestamps. Check runtime prerequisites and startup/operation durations before
claiming a meaningful comparison. Record every result; do not retry or replace failures.
Two repetitions are a bounded reproducibility check, not statistical generalization.

```sh
python3 verifiers/experiments/event-order-rejections/run.py \
  --output verifiers/target/event-order-rejections/NEW_RUN
```

The original space-map package and the qualified empty-event-delivery build are local
prerequisites. The runner checks their identities, production/build/dependency hashes,
image and launch code before and after. It does not modify package contents or manually
invent scenario IDs. Logs and joined execution/ImpactV1/ImpactV2 reports are retained.

## Comparison and checks

```sh
python3 verifiers/experiments/event-order-rejections/compare.py \
  --output verifiers/target/event-order-rejections/NEW_RUN
python3 verifiers/experiments/event-order-rejections/check_fixtures.py \
  --output verifiers/target/event-order-rejections/NEW_RUN
python3 -m unittest discover -s verifiers/experiments/event-order-rejections -v
```

`compare.py` validates the pair's static contract, joined report hashes/identities,
successful setup, zero assigned faults, actually reached target step, absence of other
failed/skipped actions, actual relative event placement and exact delivery observation.
Publisher/receiver identities are joined to the same source setup producer occurrence
in each fresh process, rather than equated just because numeric IDs happen to match.

`ORDER_DEPENDENT_REJECTION` means the same target step succeeds in one placement and
fails in the other, both selected events were actually delivered to the same fixture
receiver role, and an exact ImpactV1 invariant finding ties the rejection to that
receiver and target step. The delivery may be early or late in the rejected run; the
report retains that direction. The initial positive qualification is late rejection.
An arbitrary unassigned runtime exception is insufficient for a positive: it may be
an infrastructure failure. Broader application rejection contracts are not implemented.

`REJECTED_IN_BOTH` retains two observed target failures without inferring their business
legitimacy or equal causes. `NO_REJECTION_CONTRAST` covers two successes or other
noncontrast outcomes. `NOT_COMPARABLE` covers unavailable controls or evidence that
cannot support the positive contract. None is an ImpactV2 category or numeric score.
Repetitions are reported separately with semantic agreement, not counted as independent
new problems. No automatic selection/pair search or ordinary-executor integration exists.

`check_fixtures.py` is explicitly Quizzes-specific experiment evidence. It compares
baseline persistent projections except eight listed runtime timestamp fields, retains
those timestamps and checks that Tournament enrolment attempts occur over four minutes
before the start. It also retains final creator/participant names and enrolment counts.
These checks complement the structural pairing; the reusable comparison is not a
universal oracle for all possible clocks, prerequisites or domain acceptance conditions.

The fixed pairs and ten classifier/contract tests passed. Tests cover both contrast
directions, rejection/success in both, missing controls, empty routes, different receiver
roles, unclassified runtime failures, changed inputs/setup/routes/action order and
ambiguous/unchanged schedules. This is research orchestration, not a Java production
change, so no Java rebuild or unrelated full-suite rerun was needed.

See [measured results](../../../docs/verifiers-impl/evidence/event-order-rejections-2026-09-08/README.md).
