# Update-impact paired experiment

This disposable experiment records two paired update cases against Quizzes Saga/local. It does not change application sources: `run.sh` copies the simulator, verifier, and Quizzes modules to a temporary directory and applies each declared patch only to its matching copied build. Before compiling, the runner requires the generated source diff to equal the checked-in patch byte for byte.

The pairs are:

1. The current `UpdateQuestionEvent` path versus a one-line diagnostic repair that registers the copied Quiz as changed after changing its embedded question.
2. The current `UpdateTournamentFunctionalitySagas` recovery versus a controlled mutant that suppresses only `updateTournamentStep`'s restoring command.

Each condition starts a separate Spring JVM with a fresh H2 database. One shared, recorded fixture clock keeps paired inputs identical. Reports contain selected domain projections, action/evidence records, exact event and fault identities, lock state for the affected aggregates, and source/build/patch hashes. They intentionally contain no harm or impact score.

Run from the repository root:

```bash
SOURCE_REVISION="$(git rev-parse HEAD)"
docker compose run --rm --no-deps --pull never -T \
  -e SOURCE_REVISION="$SOURCE_REVISION" \
  --entrypoint bash scenario-executor \
  /verifiers/experiments/impact-updates/run.sh
python3 verifiers/experiments/impact-updates/validate.py \
  verifiers/target/impact-updates \
  --output verifiers/target/impact-updates/validation-summary.json
```

The runner writes four JSON reports, four full Spring logs, and `artifact-hashes.txt` under `verifiers/target/impact-updates/`. The host validator also verifies the current production source hashes, variant and patch provenance, all per-case checks, and paired fixture/route identity.

This is bounded diagnostic evidence. The Question case demonstrates durable no-progress after successful processing in the current application and convergence in the repaired copy. The Tournament mutant demonstrates what the existing compensation prevents; it is not evidence of a current application defect. The projections retain relevant domain fields and dates, but do not serialize the complete application state or framework `creationTs`.
