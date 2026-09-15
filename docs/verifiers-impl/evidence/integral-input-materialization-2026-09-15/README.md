# Integral participant input materialization

The 156-candidate workload's input preparation defect is fixed. The application itself
and the persisted source-derived package are unchanged.

## Cause and fix

CreateTournament argument 4 is a TournamentDto containing `numberOfQuestions = 2`.
The current package reader used this Java conditional for integral JSON values:

```java
value.canConvertToInt() ? value.asInt() : value.asLong()
```

Java promotes the branches to `long` before boxing. Even the small integer became Long,
which reflection could not pass to `setNumberOfQuestions(Integer)`. The outer diagnostic
reported an argument type mismatch at argument 4. Source setup had succeeded because
its separate typed binding path already converts numeric values.

The reader now preserves Jackson's integral Number representation, including wide values.
Participant DTO assignments bind numeric values to the declared integral destination
exactly. This preserves small Long fields as well as Integer setters; fractional and
out-of-range conversions fail instead of truncating. No application-specific conversion,
new setup recipe, business-code change or impact-policy change was introduced.

## Regression evidence

The package roundtrip regression reproduced both the boxed/primitive dummyapp assignment
failure and the incorrect runtime types before the fix. Exact Integer/Long limits and
BigInteger values beyond Long remain covered. Assignment tests cover a small Long value,
overflow and a fractional value targeting Integer. An existing overload-search test had
accidentally relied on small literals becoming Long; it now uses a value genuinely outside
Integer's range to preserve the intended fallback test.

273 executor/package/artifact regression tests pass. This includes 217 ScenarioExecutor
cases. The initial test-development run had a test accessor typo, corrected before the
real red/green comparison; the meaningful pre-fix run had six failures.

## Runtime qualification

The original failed-control package was replayed with the same frozen application and a
new hash-checked overlay for only ScenarioCatalogPackageReader and ScenarioMaterializer.
All three participants now materialize and reach startup readiness. The no-fault scenario
reaches CreateTournament's final step, where the application rejects an invariant. It ends
PARTIAL_COMPENSATED with DEVIATED conformance; the four enabled criteria are complete and
score zero. This is an executed no-fault history, not an entirely successful user operation.

Its forward order anonymizes the creator before creating the Tournament. ExecutionService
returns the enrolled student's DTO; Tournament forbids an anonymous creator while active.
This explains the observed rejection consistently with the source. The exception itself
contains only the aggregate-level invariant error, not a per-predicate report.

The new copied-update criterion remains unavailable for this old package because it lacks
copy contracts; it is disabled in the retained four-weight policy. No unavailable count is
converted to zero.

A small random sample was declared before execution: seed 15156, budget 12, recovery cap
500, population 8, mutation 0.3, the original four unit weights and unchanged frozen clock.
All 12 unique executions had complete four-criterion fitness zero; no score was
unavailable. Exact terminal states and vectors are retained in [summary.json](summary.json).
The previously qualified generated lost-copy positive was also replayed: SUCCESS/EXACT,
I=0, read A=0 and copied-update count 1 with complete coverage. This qualification does not establish
variation across all 156 candidates and is not a new GA/random comparison.

## Reproduction

Raw evidence: `verifiers/target/integral-input-materialization-01/`. It retains the copied
sources/classes, their hashes in config.json, package snapshots, logs and report joins.
The baseline failed run remains at
`verifiers/target/workload-cohort-exploration-01/candidate-6-control/`.

```sh
python3 verifiers/experiments/fixed-workload-ga/run.py control \
  --config verifiers/target/integral-input-materialization-01/config.json \
  --output verifiers/target/integral-input-materialization-01/control-repeat
python3 verifiers/experiments/fixed-workload-ga/run.py run \
  --config verifiers/target/integral-input-materialization-01/config.json \
  --control verifiers/target/integral-input-materialization-01/control/control.json \
  --output verifiers/target/integral-input-materialization-01/sample-repeat
```
