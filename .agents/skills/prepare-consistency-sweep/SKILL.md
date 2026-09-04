---
name: prepare-consistency-sweep
description: Onboard a simulator application into the repository's consistency-testing engine. Use when an agent must discover saga aggregates and WorkflowFunctionality classes, create neutral FunctionalityCatalogsProvider and InterInvariantsProvider test beans, add Maven consistency-sweep wiring, generate coverage evidence and validation tests, or prepare an application for its first consistency sweep. Do not use to interpret completed sweep results; use review-consistency-sweep explicitly instead.
---

# Prepare Consistency Sweep

Create reviewable, evidence-backed adapter between one simulator application and `consistency-testing`. Keep baseline neutral: inventory coverage without designing scenarios around suspected bugs.

## Required references

Read before editing:

- [integration-contract.md](references/integration-contract.md) for engine contracts, Maven wiring, and semantic traps.
- [coverage-manifest.md](references/coverage-manifest.md) for required coverage evidence.

Use templates under `assets/` as starting points. Replace `appName` with generated adapter prefix, `applicationClass` with fully qualified Spring entry point, and `applicationSimpleClass` with its simple name. Adapt other placeholders; remove every template `TODO` before completion.

## 1. Resolve scope and protect existing work

1. Anchor to repository root with `git rev-parse --show-toplevel`.
2. Read root `AGENTS.md` and any `AGENTS.md` governing target application.
3. Require explicit target under `applications/<app-name>`. Do not guess among multiple applications.
4. Run `git status --short` before writes.
5. If worktree is dirty, stop and list paths. Continue only after explicit user authorization. Record baseline dirty paths and never edit them unless target work necessarily overlaps and user separately approves overlap.
6. Never stage or commit. Leave all changes unstaged.

Scope is Maven, Spring, and saga-profile applications built on this repository's simulator. Halt on another framework or consistency profile.

## 2. Run deterministic discovery

Run:

```bash
python .agents/skills/prepare-consistency-sweep/scripts/discover_application.py applications/<app-name>
```

Treat output as candidates, not truth. It finds Spring entry points, saga aggregates, `WorkflowFunctionality` subclasses, test helpers, and existing integration wiring. Inspect source, constructors, application services, tests, events, commands, and domain docs to confirm meaning and preconditions.

Do not use another application's catalog selection as template. They can contain planted, hypothesis-driven scenarios and not be neutral coverage baseline.

## 3. Build complete coverage inventory

Create `src/test/resources/consistency-testing/catalog-coverage.yaml` from [catalog-coverage.yaml.template](assets/catalog-coverage.yaml.template).

Inventory every discovered saga aggregate and `WorkflowFunctionality`, including reads. Record explicit aggregate relationships from code or docs without speculating about consistency risk. For each functionality record source class and kind, inclusion status, catalog and stable functionality ID when included, evidence used, and explicit exclusion reason when excluded. Never silently omit candidate.

Treat functionality as application surface when a controller, command mapping, coordination `*Functionalities` facade, or ordinary application test invokes it. Include surface operations by default. A production class used only by consistency experiments, not exposed through application behavior, may be excluded with evidence. When exposure remains ambiguous, mark excluded with `application surface unresolved` rather than guessing.

## 4. Design neutral catalogs

Use production code, existing tests, and domain docs as evidence. Do not infer likely concurrency defects or construct adversarial states around them.

For each included operation:

1. Create valid shared initial state using public application behavior where possible.
2. Register every stable pre-existing aggregate identity in `AggregateHandlesRegistry`.
3. Resolve run-local aggregate IDs from handles inside each factory.
4. Return fresh `WorkflowFunctionality` instance and fresh unit of work each run.
5. Give distinct IDs to multiple concrete instances in one catalog.

Catalogs represent incompatible initial states, not aggregates or services. Keep one catalog when possible. If partitioning is required, record cross-catalog pairs that become untestable. Duplicate functionality across catalogs when different valid states materially expand coverage.

Create application-specific test helper only when repeated valid-state or functionality construction warrants one. Never cache helpers holding injected beans: engine may replace beans between runs.

## 5. Add only explicit inter-invariants

Create `*InterInvariantsProvider` using [InterInvariantsProvider.java.template](assets/InterInvariantsProvider.java.template).

Implement invariant only when code comments, tests, or domain docs state it explicitly. Cite evidence in manifest. Implement every explicit cross-aggregate invariant whose current state can be observed reliably without changing application behavior. Record explicit but unimplementable invariants under `unimplemented_explicit_inter_invariants` with reason; never silently omit them. When no invariant is explicit, return `Set.of()`.

Record plausible but uncertain invariants under `candidate_inter_invariants`; do not generate code and do not ask user unless progress is otherwise blocked.

## 6. Generate integration files

Create under target application's `src/test/java` package:

- `*FunctionalityCatalogsProvider`, based on [FunctionalityCatalogsProvider.java.template](assets/FunctionalityCatalogsProvider.java.template);
- `*InterInvariantsProvider`, based on [InterInvariantsProvider.java.template](assets/InterInvariantsProvider.java.template);
- `*ConsistencyCatalogValidation`, based on [ConsistencyCatalogValidation.java.template](assets/ConsistencyCatalogValidation.java.template);
- `*ConsistencySweep`, based on [ConsistencySweep.java.template](assets/ConsistencySweep.java.template).

Patch `pom.xml` idempotently using [pom-snippets.xml](assets/pom-snippets.xml):

- add test-scoped `ConsistencyTesting` dependency only if absent;
- add `consistency-sweep` profile only if absent;
- include validation and sweep classes explicitly;
- do not add `maven-jar-plugin:test-jar`; it supports reverse test-fixture dependencies, not ordinary onboarding.

Generated sweep must read `consistency.iterations` with default `20`, optionally read `consistency.masterSeed`, log summary, and make no generic finding assertion.

## 7. Validate without running full sweep

Install local prerequisites when needed:

```bash
mvn -f simulator/pom.xml install -DskipTests
mvn -f consistency-testing/pom.xml install -Dmaven.test.skip=true
```

Then, from target application:

```bash
mvn test-compile -Pconsistency-sweep
mvn test -Pconsistency-sweep -Dtest=<App>ConsistencyCatalogValidation
```

Compilation and solo profiling must pass. Do not run concurrent sweep during onboarding. If discovered operation cannot solo-profile, fix state/factory or exclude it with precise evidence; never hide failure.

## 8. Hand off

Show files changed and unstaged status, coverage counts per catalog, confirmed/candidate/unimplemented-explicit inter-invariants, validation results, and lost cross-catalog coverage.

End with user call to action:

```bash
mvn test -Pconsistency-sweep -Dconsistency.iterations=1
mvn test -Pconsistency-sweep
mvn test -Pconsistency-sweep -Doracle.db=postgres
```

Explain first command is cheap end-to-end smoke, second is default sweep, third uses PostgreSQL and requires Docker. Offer explicit `$review-consistency-sweep` after completion. Do not start review implicitly.
