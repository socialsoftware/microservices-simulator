# Empty selected event delivery qualification

This fixed comparison exercises the explicit `NO_ELIGIBLE_SUBSCRIBER` replay outcome.
It reuses retained packages without changing their actions, inputs, IDs or setup.
It is not a rerun of the full forty-invalid campaign and does not synthesize receivers.

The six attempts are selected before observing new outcomes:

| Case | Purpose |
| --- | --- |
| Corrected w01, no fault | Continue after empty QuizAnswer and Tournament routes |
| UpdateStudentName to Tournament, no fault | Preserve a real healthy delivery |
| Same Tournament workload, trigger fault | Preserve masking when no event is produced |
| RemoveCourseExecution, QuizAnswer then Quiz then Tournament | Preserve two actual deliveries and continue past the absent third receiver |
| Existing UpdateQuestion qualification positive | Preserve the unresolved delivered-event finding |
| Corrected w01, observer disabled | Confirm observation does not change execution |

The Question positive reuses the unchanged provider in
`../impact-v2/fixtures/QuizzesImpactV2PrerequisiteProvider.java` and its retained package.
It is qualification-only and provider-backed, not a source-extracted input. The provider
is copied into the private application test tree. Quizzes production is unchanged; the
other cases use existing source-derived setups. The recorded fixture instant is UTC;
prepare and execute within its two-hour course window, or create a fresh selection/run.

From the repository root, choose a fresh output directory under `verifiers/target`.
`plan` freezes the selected cases and original package file hashes. `prepare.sh` runs in
the existing Compose image and creates isolated source, classes and classpaths; pass a
private Maven repository to avoid sharing mutable artifacts. A previously populated
dependency cache can be copied into that private repository before preparing.

```sh
python3 verifiers/experiments/empty-event-delivery/qualify.py plan \
  --output verifiers/target/empty-event-delivery/NEW_RUN

docker compose -p microservices-simulator -f docker-compose.yml run \
  --rm --no-deps --pull never -T \
  -e BUILD_OUTPUT_DIR=/reports/empty-event-delivery/NEW_RUN/prepared-build \
  -e 'JAVA_TOOL_OPTIONS=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dmaven.repo.local=/reports/empty-event-delivery/NEW_RUN/m2' \
  scenario-executor bash /verifiers/experiments/empty-event-delivery/prepare.sh

python3 verifiers/experiments/empty-event-delivery/qualify.py run \
  --output verifiers/target/empty-event-delivery/NEW_RUN
python3 verifiers/experiments/empty-event-delivery/qualify.py summarize \
  --output verifiers/target/empty-event-delivery/NEW_RUN
```

Each attempt uses a fresh disposable container, JVM and H2 database. Attempts run
serially with a 180-second per-attempt guard. Reports/logs remain under the output;
`comparison.json` validates continuation, no fabricated deliveries, zero contribution
from empty selection, the known positive, masking and observer parity. It verifies
original package hashes again after execution. Preserve failures instead of rerunning
into an existing case directory. Historical reports retain their old replay policy.
