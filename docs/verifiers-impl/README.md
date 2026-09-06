# Verifier documentation

This directory documents the current verifier and fault-analysis scenario pipeline.

## Reading order

For the advisor discussion, start with the [European Portuguese meeting note](reunioes/2026-09-08.md).
It explains the domain and develops the first control/fault case in full; the remaining
case sheets await review of that format. The handbook below remains the authority for
the implementation contract and complete technical evidence.

1. [`current-state.md`](current-state.md) — canonical handbook: purpose, concepts, inputs, the current manifest-described package, metrics, operations, current evidence, commands, limits, and safe claims.
2. [`roadmap.md`](roadmap.md) — detailed future outcomes and their completion boundaries.
3. [`decisions/`](decisions/index.md) — only when you need the rationale behind a non-obvious active design choice.

Do not start from old issue packages or Git history when answering a current-behavior question. They preserve chronology, not current truth.

## Find an answer

| Question | Read |
|---|---|
| What will we discuss with the advisor, in Portuguese? | [Meeting note — 8 September](reunioes/2026-09-08.md) |
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
