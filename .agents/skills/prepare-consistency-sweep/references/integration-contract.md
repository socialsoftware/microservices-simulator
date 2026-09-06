# Consistency-testing integration contract

## Required application beans

Both beans live under application `src/test/java`, annotated with `@Component` and `@Profile("oracle")`.

`FunctionalityCatalogsProvider` returns one or more `FunctionalityCatalog` values. Each catalog has:

- unique nonblank name;
- initial-state supplier invoked once per oracle run;
- `AggregateHandlesRegistry` mapping stable logical handles to run-local IDs;
- map of stable `FunctionalityId` values to factories creating fresh `WorkflowFunctionality` instances.

`InterInvariantsProvider` returns confirmed cross-aggregate invariants. Empty `Set.of()` is mechanically valid when none are explicitly documented.

## Catalog semantics

- Database is cleared after every oracle run. Never retain concrete IDs across runs.
- Handle and aggregate ID are each one-to-one within a registry.
- Factories resolve handles only after current run's initial state exists.
- Duplicate map keys inside one catalog silently replace earlier entries. IDs must be unique.
- Engine profiles each functionality alone to observe read/write footprint, then plans pairs.
- Conditional accesses absent from solo path can be missed.
- Planner explores pairs, not higher-order concurrency.
- Catalogs are planned independently. No pair crosses catalog boundary.
- Unregistered aggregates created mid-run use type-level wildcard identities. This is safe over-approximation but may add runs.

Partition catalogs only for incompatible initial states. Architectural partitioning by aggregate or service loses cross-aggregate pair coverage.

## Neutral baseline

Inventory all `WorkflowFunctionality` subclasses. Use existing successful tests, application services, commands, events, and comments to construct ordinary valid scenarios. Do not choose states because they seem likely to expose a race.

Include reads as candidates: read/write conflicts can matter. Application surface is evidenced by controller, command mapping, coordination `*Functionalities` facade, or ordinary application-test use. Exclude only with explicit reason such as unsupported profile, duplicate internal helper, impossible construction from available application API, operation outside application surface, or unresolved exposure.

Record uncertain inter-invariants as candidates only. Implement invariant only when code comments, tests, or docs state it. Account for every explicit cross-aggregate invariant: implement it when reliably observable, otherwise record it as explicit but unimplemented with reason.

## Supporting files

Typical application integration contains functionality catalogs provider, inter-invariants provider, optional state/functionality helper, solo validation test, full sweep test, coverage manifest, test dependency, and Maven profile.

Do not publish application's test jar unless another module explicitly consumes those test classes.

## Maven wiring

Add test dependency:

```xml
<dependency>
    <groupId>pt.ulisboa.tecnico.socialsoftware</groupId>
    <artifactId>ConsistencyTesting</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

Add `consistency-sweep` profile with `activatedProperties` set to `test,sagas,local` and Surefire includes for validation and sweep classes. Keep them outside ordinary build naming patterns.

## Validation behavior

`mvn test-compile -Pconsistency-sweep` catches Maven, import, constructor, and syntax problems.

`TestDriver.init()` boots application with oracle profile and requires both providers. `profileFunctionalities(catalog)` runs every entry alone once. It validates setup, handles, factory construction, execution, and confirmed invariants.

Neutral catalog entries must complete successfully in solo profiling. Step
exceptions, including normal business rejections, and internal or critical
failures, execution limits, dependency-resolution failure, or inter-invariant
violations invalidate an entry. The profiler attempts every entry and reports
all invalid entries before rejecting the catalog. An operation only made valid
by concurrent progress belongs in an explicit targeted experiment, not a
neutral catalog.

Full exploration findings are evidence, not automatic test failures. Generic sweep must not assert no findings: business exceptions can count as findings, and application may legitimately have zero planned groups.
