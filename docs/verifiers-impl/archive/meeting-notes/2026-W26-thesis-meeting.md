# Thesis meeting notes — 2026-W26

## Summary

I discussed current trouble around ScenarioExecutor/input materialization. The advisor asked for next week’s meeting to be in person, in his office, as a full zoom-out/recap of the verifier work.

## Advisor request for next week

Prepare a broad recap covering:

- what has been done;
- how the pieces fit together;
- what is currently being worked on;
- what remains to be done;
- what questions are open;
- what is not working well;
- what is well designed;
- what is not well designed;
- how to run the actual programs, verifier flows, and checks.

## Current concern raised

Materialization is becoming the hard part: converting static catalog inputs, recipes, placeholders, Spring/runtime dependencies, and runtime-produced values into live objects that a scenario executor can use reliably.

This should be framed as a concrete design boundary, not as vague confusion. The static catalog can describe many candidate inputs, but generic execution requires deciding which recipe shapes are supported, which values must be produced by setup execution, and which unresolved placeholders remain blockers.

## Preparation plan

Use the active docs, not the archive, as the source of truth:

- [`../../current-state.md`](../../current-state.md)
- [`../../advisor-brief.md`](../../advisor-brief.md)
- [`../../roadmap.md`](../../roadmap.md)
- [`../../evidence.md`](../../evidence.md)
- [`../../reference/scenario-executor.md`](../../reference/scenario-executor.md)

Prepare a short in-person walkthrough rather than rereading every historical note. The goal is to explain the system, show what runs, identify weak spots, and ask for direction on the next thesis milestone.

## Things to be ready to explain

- Static verifier: source/test extraction, source-mode filtering, scenario catalog, accounting.
- Dynamic enrichment: runtime evidence, exact attribution, remaining ambiguous/unmatched cases.
- Segment compression: why it reduces schedule explosion and what it does not prove.
- ScenarioExecutor POC: what it can run now and why generic materialization is still hard.
- Remaining thesis stages: generic execution, fault injection, impact scoring, search/prioritization.

## Likely advisor questions

- What is the current end-to-end runnable path?
- Which outputs are thesis evidence versus diagnostics?
- How much of the executor must be generic for the thesis evaluation?
- What is the smallest credible next milestone?
- Which parts are well designed enough to keep, and which should be simplified?
