# Verifier documentation

This directory documents the current verifier and fault-analysis scenario pipeline.

## Reading order

Start from the current implementation and remaining work below. The Portuguese meeting
notes explain the domain and experiments discussed on [8 September](reunioes/2026-09-08.md)
and [11 September](reunioes/2026-09-11.md); they are dated discussion material, not the
current implementation status.

1. [`current-state.md`](current-state.md) — canonical handbook: purpose, concepts, inputs, the current manifest-described package, metrics, operations, current evidence, commands, limits, and safe claims.
2. [`roadmap.md`](roadmap.md) — current priorities, remaining outcomes and conditional follow-ups.
3. [`decisions/`](decisions/index.md) — only when you need the rationale behind a non-obvious active design choice.

Do not start from old issue packages or Git history when answering a current-behavior question. They preserve chronology, not current truth.

## Find an answer

| Question | Read |
|---|---|
| Can copied-value overwrites be observed without manual application field mappings? | [Integrated detector qualification](evidence/lost-copied-update-2026-09-15/README.md); [earlier inference proof](evidence/inferred-stale-write-2026-09-15/README.md) |
| Where are the Portuguese explanations used in advisor discussions? | [11 September note](reunioes/2026-09-11.md); [domain and earlier cases — 8 September](reunioes/2026-09-08.md) |
| What is the larger GA/random comparison, and is it finished? | [Recovery-qualified campaign](evidence/recovery-history-2026-09-15/README.md); live status in `verifiers/target/ga-500x3-2026-09-15/status.json` |
| Which smaller benchmark has a complete positive reference map? | [29-case reference and discovery experiment](evidence/ga-discovery-2026-09-10/README.md) — its denominator does not apply to the larger campaign |
| How do I search one workload with GA/random and replay a result? | [Fixed-workload search](../../verifiers/experiments/fixed-workload-ga/README.md) and [qualification](../../verifiers/experiments/fixed-workload-ga/RESULTS.md) |
| How do I weight individual impact/anomaly criteria or revalue saved results? | [Configurable fitness](../../verifiers/experiments/fixed-workload-ga/README.md#configurable-fitness) |
| How do candidate combined scores compare on actual results? | [Earlier offline matrix](evidence/impact-anomaly-matrix-2026-09-10/README.md) and [configurable-fitness qualification](evidence/weighted-fitness-2026-09-15/README.md); weights are user preferences, not inferred severity |
| Which Saga anomalies and literature should inform the next impact work? | [Exploratory theory and simulator mapping](research/saga-anomalies.md) — research, not an approved implementation contract |
| What changes when several listeners process the same event? | [Combined-event qualification](current-state.md#combined-event-qualification) and [Portuguese case](reunioes/2026-09-08.md#32-a-remocao-termina-mas-falta-avisar-os-objetos-dependentes) |
| What does the current potential-impact score measure? | [ImpactV2 contract](current-state.md#impactv2-assessment) and [latest qualification](current-state.md#owned-cycle-coverage-and-control-requalification) |
| How do Quizzes, the four runs, and the impact question fit together? | [`Current state — Quizzes impact example`](current-state.md#understanding-impact-through-quizzes) |
| What does the verifier determine? | [`Current state — short version`](current-state.md#the-short-version) |
| What do accepted, setup candidate, setup-ready, WorkloadPlan, and FaultScenario mean? | [`Current state — essential terms`](current-state.md#the-essential-terms) |
| What roles and files make up the current package? | [`Current state — current package`](current-state.md#the-current-package) |
| What do scenario-space, strict/broad, and recovery metrics mean? | [`Current state — accounting`](current-state.md#how-to-read-accounting) |
| How does source/test input extraction work? | [`Current state — inputs and static extraction`](current-state.md#inputs-and-static-extraction) |
| What does dynamic enrichment add? | [`Current state — dynamic evidence`](current-state.md#optional-dynamic-evidence) |
| How do preflight, execution, and impact differ? | [`Current state — ScenarioExecutor`](current-state.md#scenarioexecutor), [ImpactV1](current-state.md#impactv1), and [ImpactV2](current-state.md#impactv2-assessment) |
| What current bounded Quizzes evidence exists? | [`Current state — current evidence`](current-state.md#current-evidence) |
| What comes next, and which earlier follow-ups remain open? | [`Roadmap — current next work`](roadmap.md#current-next-work) |
| Why was a durable design choice made? | [`Decisions`](decisions/index.md) |

## Evidence policy

`current-state.md` keeps the latest representative evidence for each current capability. Each evidence section should include:

- the question being tested;
- the exact command and relevant configuration;
- the output artifact path;
- the discriminating result;
- what that result proves and does not prove.

Replace superseded baselines instead of appending historical chronology. Git history is the engineering archive.

## Documentation policy

- `current-state.md` is the sole owner of present behavior, essential terminology, evidence, and limitations.
- `roadmap.md` owns future direction and does not restate current evidence.
- Decision records own only durable rationale that is likely to matter again.
- Explain the domain, ordinary workflow, controlled intervention, final observation,
  interpretation, and next unresolved question before implementation details.
- For experiments, state why each control exists and distinguish observed facts from
  domain-harm claims. Keep commands and exact evidence scope available after the story.
- Define terms at first use; do not create a parallel glossary.
- Do not maintain manual "Last updated" labels. Use Git for revision history; retain
  dates only when they identify an experiment, decision, or other meaningful event.
- On bounded changes, update the affected current explanation, discriminating evidence,
  and next unresolved question. Use issue packages for coordination and decisions;
  do not create a new document solely to narrate a small completed edit.
- Delete stale implementation logs, investigations, prompts, and meeting material rather than placing them in an in-repository archive.
- Keep implementation details in source and tests unless they are required to operate the system or interpret a thesis claim.

## Preview

From the repository root:

```bash
./scripts/verifier-docs serve
```

Build the static site:

```bash
./scripts/verifier-docs build
```
