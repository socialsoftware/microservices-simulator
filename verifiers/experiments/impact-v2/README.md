# Persisted ImpactV2 qualification fixture

This directory defines the bounded M2 qualification for
`issues/2026-09-06-potential-impact-v2`. It creates one deterministic current package
with three provider-backed Quizzes workloads and executes selected persisted
`FaultScenario` IDs through the ordinary `ScenarioExecutorCli` path.
The completed measurements and failed-trial record are kept in
[`M2-HANDOFF.md`](../../../issues/2026-09-06-potential-impact-v2/M2-HANDOFF.md), outside
the source trees hashed by the final run.

The package is deliberately labeled
`qualification-only-provider-backed-not-source-extracted`. It fills the executable
`UpdateTournament` and `UpdateQuestion` catalogue gap without changing the source
analyzer, generator readiness rules, or production Quizzes code. The provider is copied
into temporary application test sources and calls the real application facades before
measurement. Every case runs in a new JVM with a new in-memory H2 database.

The three families are:

- deleted dependency: `RemoveTournament`, with an assigned fault before the
  `removeTournamentStep` body and the matching all-zero successful-removal control;
- failed update residual: `UpdateTournament`, with an assigned fault before the
  `updateQuizStep` body on the current build and an exact temporary-copy mutant whose
  existing compensation is a no-op, plus all-zero controls on both builds;
- unresolved delivered event: all-zero `UpdateQuestion` followed by the selected exact
  `UpdateQuestionEvent` delivery, on the current build and an exact temporary-copy
  `registerChanged` repair.

`generate-fixture.groovy` uses the production model, ID, eager fault/recovery generator,
and `ExecutableArtifactWriter` APIs. It writes `selection.json` beside the ordinary
current-package files so the runner never constructs a runtime vector or hand-edits a
report. The two patches are byte-for-byte copies of the trusted patches in
`../impact-updates/patches/`; the runner checks unique source anchors and compares the
actual temporary-copy diffs to those files before compiling.

Do not run this while shared simulator or verifier sources are changing. Once M0/M1 are
stable, run only the disposable Compose service and reuse the main project Maven cache:

```bash
docker compose \
  -p microservices-simulator \
  -f /Users/andre/meic/thesis/microservices-simulator-impact-v2/docker-compose.yml \
  run --rm --no-deps --pull never -T --entrypoint bash \
  -e SOURCE_REVISION="$(git -C /Users/andre/meic/thesis/microservices-simulator-impact-v2 rev-parse HEAD)" \
  scenario-executor \
  -lc 'command -v python3 >/dev/null || { apt-get update && apt-get install -y --no-install-recommends python3; }; exec /verifiers/experiments/impact-v2/run.sh /reports/impact-v2'
```

The stock runtime image currently lacks Python, so the disposable container installs
the interpreter needed by the fixture selector, metrics recorder, and strict validator.
This changes only that one `--rm` container; it does not commit an image or alter another
running service.

The runner is serial and caps Maven heap use. It does not restart or stop any other
Compose service. Its output includes the package, eight core execution reports, ImpactV1
reports, automatically derived `*.execution.impact-v2.json` reports, per-case logs,
read-only `*.state-witness.json` projections, hashes, and `validation.json` under
`verifiers/target/impact-v2/` on the host. The witness captures only the fixture's
Tournament, Quiz, and Question at Spring context shutdown; it never feeds the score or
invokes a business mutation.

After all three application variants compile, the runner records one current UTC instant
and supplies it to every provider invocation. Set `FIXTURE_NOW` only when intentionally
replaying an earlier fixture instant. `source-content-manifest.tsv` hashes every source
file in the mounted and temporary simulator, verifier, and application trees, excluding
Git metadata, build outputs, and generated runtime logs. Validation proves the copies match their mounted inputs
and that the application variants differ only by the injected provider and their one
declared patch target.

ImpactV2 collection is installed on every ordinary attempt. The runner also adds three
fresh observer-off executions, one per family, with the supported
`-Dmicroservices.simulator.impact.enabled=false` switch. The validator requires each
disabled sidecar to say `UNAVAILABLE`/`COLLECTION_DISABLED` and compares its application
outcome and action trace with the corresponding observer-on run.
`cases.tsv` records each fresh JVM's wall-clock duration, report sizes, witness size, and
evidence counts. The validation report presents enabled/disabled duration ratios only as
single-run descriptive observations; they are not benchmark estimates.

The expected scored results are category-specific. The assigned deletion case has a
union score of two: the active Tournament with a deleted declared Quiz dependency and
the deleted Quiz left by the failed Saga. Both assigned update builds have one residual
Tournament under the approved full persistent-data rule. Normal recovery restores the
question count and start/end dates, but changes `lastModifiedTime` and loses the restored
topics' `topicCourseAggregateId` values. The no-op variant additionally leaves the new
question count, dates, and third topic in place. Their equal scalar score illustrates a
known limit: this count detects a potential pattern, but does not grade its harm. A
domain-owned distinction for metadata would require a separate product decision. The
current event build has one unresolved Quiz receiver. All unassigned and repaired-event
controls must be complete zeros; an unknown or partial assessment never passes as zero.
