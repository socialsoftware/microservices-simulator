# Fault Lab — local experiment workbench

Run from the repository root with **Python 3.10+ on macOS or Linux**:

```sh
python3 verifiers/webui/server.py
```

Open <http://127.0.0.1:8765>. No Python packages, Node build, CDN, account or external
service is required. `--port 8766` selects another loopback port. Repeat `--root
/absolute/artifact/directory` to include additional trusted local evidence directories.
The service is for one trusted local researcher, not public hosting or multiuser access.

## Try it

1. Open an existing run in **Experiments**. Inspect discovery curves, allocation by
   workload, **Scenario evidence**, and **Impact breakdown**. Switch the curve between
   positives, cumulative configured score, and unavailable feedback.
2. Search/filter the history and check up to four runs, then open **Compare runs**.
   These are individual seed traces, not an aggregate or statistical significance test.
   The context warning remains when scope/fitness/provenance is different or incomplete.
3. Download the plot as SVG, per-attempt rows as CSV, or the normalized run including
   retained observations and provenance as JSON. CSV leaves unavailable scores empty.
4. Click **New experiment**, choose a prepared allocation configuration, select
   workloads, and set policy, budget, seed and the five impact weights. **Check inputs**
   checks settings and path availability; **Launch** invokes the existing runner, which
   performs the full catalogue/control/runtime qualification before search.
5. Follow the job and log in **Live activity**. Recorded-feedback allocation publishes
   results at completion. Live allocation exposes completed decisions and in-flight work;
   pause drains the current attempt, and resume uses the runner's existing integrity gates.
   Closing the browser or this HTTP service does not stop a detached job. Reopening the
   service restores its registry from disk. Host sleep or shutdown still stops execution.

## Supported inputs and boundaries

The default scanner reads `verifiers/target`, to at most seven nested directory levels.
It excludes attempt/package/build trees, does not follow directory symlinks, and limits
each decoded artifact to 64 MiB. Unreadable artifacts are counted in the index status.
**Refresh** discovers external changes. Results produced by workbench jobs refresh
automatically, including new results from an existing resumed job.

Supported retained results:

- Fixed-workload `results.json` with an attempt list and strategy. Configured
  `fitnessScore` is authoritative. Explicit historical
  `complete-impact-v2-object-count` traces can instead retain their legacy score in `I`;
  the viewer labels that policy and does not reinterpret it using today's weights.
- Allocation `results.json` with its adjacent `decisions.jsonl`.
- Research `seed-*.json` / `seed-*.json.gz` allocation traces with embedded decisions.
  Missing fitness, mode, or component evidence remains explicitly unavailable. The
  compressed research trace adapter assumes recorded feedback only when no mode exists.

The index is a reader, not a new integrity certification of historical evidence. It
does not recompute scientific assessments or change their versions. Positive scenarios
are not a count of independent defects. Unknown feedback consumes budget but is not zero.
Impact breakdown sums retained available component counts across attempts, with their
observation counts; components can overlap. Compact allocation traces generally contain
scores only, so they do not acquire fabricated impact breakdowns or action schedules.
Fixed-workload comparison compatibility remains unknown because historical runs do not
uniformly preserve a sealed source/runtime/domain identity in their summary contracts.

Supported **launch** configurations are the existing public
`contextual-workload-allocation.v1` and `contextual-workload-live-allocation.v1` files.
Place a `*config*.json` or `live-*.json` under an indexed root and refresh. Their existing
workload entries reference prepared map configurations, complete catalogues, and either
recorded references or qualified no-fault controls. Path availability alone is not proof
that a configuration will pass its runner's integrity checks. UI method choices are
the eight existing core CLI policies; experimental research-only hybrid/order policies
remain viewable as results but are not newly exposed as live CLI implementations.

The workbench does not build applications, generate/qualify catalogues and controls,
submit cluster jobs, support multiuser scheduling, or introduce new verifier semantics.
It does not launch historical single-workload runners from the UI in this first version.
SVG curves are exploratory exports; existing study-specific plotting/analysis scripts
remain responsible for publication figures and multi-seed statistical summaries.

## Job storage and recovery

New jobs get fresh directories under `verifiers/target/webui/jobs/<id>/`:

- `configuration.json`: settings snapshot with selected workload references resolved.
- `job.json`: detached supervisor state and exact allowlisted runner argument array.
- `runner.log` / `supervisor.log`: retained process output.
- `output/`: the runner's ordinary evidence, journal, receipts and reports.

Existing artifacts are never overwritten. At most one workbench job can run at a time;
dispatch locks coordinate multiple HTTP instances and the worker lock is inherited by
its runner. External CLI jobs are outside this registry. Live pause/resume only applies
to workbench-owned jobs. A crashed supervisor is shown as interrupted rather than
silently retried; ambiguous live dispatches remain subject to the existing CLI's
`REVIEW_REQUIRED` / explicit resolution workflow. The UI does not resolve them for you.

HTTP binds to `127.0.0.1` only, validates the Host and mutation Origin, requires an
unpredictable mutation token, exposes only fixed static routes and registered artifact
IDs, and launches subprocesses with argument arrays rather than shell commands.

## Validation

```sh
python3 -m unittest discover -s verifiers/webui -p 'test_*.py' -v
node --check verifiers/webui/static/app.js  # optional development syntax check
node verifiers/webui/test_refresh.js       # polling regression checks
```

Tests cover historical score semantics, unknown feedback, compressed traces, partial
journals, scope/provenance limits, launch validation and snapshots, detached supervisor
failure retention, duplicate launch rejection, result refresh identity, and loopback
request protection. They do not start Docker or modify existing evidence.
