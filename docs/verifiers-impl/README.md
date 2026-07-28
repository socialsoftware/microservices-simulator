# Verifier documentation

This directory documents the current verifier and fault-analysis scenario pipeline.

## Reading order

1. [`current-state.md`](current-state.md) — canonical handbook: purpose, concepts, inputs, five-file v3 package, metrics, operations, latest evidence, commands, limits, and safe claims.
2. [`roadmap.md`](roadmap.md) — detailed future outcomes and their completion boundaries.
3. [`decisions/`](decisions/index.md) — only when you need the rationale behind a non-obvious active design choice.

Do not start from old issue packages or Git history when answering a current-behavior question. They preserve chronology, not current truth.

## Find an answer

| Question | Read |
|---|---|
| What does the verifier determine? | [`Current state — short version`](current-state.md#the-short-version) |
| What do accepted, setup candidate, setup-ready, WorkloadPlan, and FaultScenario mean? | [`Current state — essential terms`](current-state.md#the-essential-terms) |
| What are the five v3 files? | [`Current state — v3 package`](current-state.md#the-v3-package) |
| What do scenario-space, strict/broad, and recovery metrics mean? | [`Current state — accounting`](current-state.md#how-to-read-scenario-space-accounting) |
| How does source/test input extraction work? | [`Current state — inputs and static extraction`](current-state.md#inputs-and-static-extraction) |
| What does dynamic enrichment add? | [`Current state — dynamic evidence`](current-state.md#optional-dynamic-evidence) |
| How do preflight, execution, and impact differ? | [`Current state — ScenarioExecutor`](current-state.md#scenarioexecutor) and [`ImpactV1`](current-state.md#impactv1) |
| Where did the latest `82/82` result come from? | [`Current state — 82/82 evidence`](current-state.md#quizzes-setup-preflight-8282) |
| What comes next? | [`Roadmap`](roadmap.md) |
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
- Define terms at first use; do not create a parallel glossary.
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
