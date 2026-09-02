# M0 handoff — semantic command root keys

- **State:** complete; milestone review passed.
- **Outcome:** command creation sites now use a per-public-constructor semantic path to
  `Command.rootAggregateId` instead of assuming argument index 2. Direct parameters,
  one direct DTO getter/property, exact literals (including signed numerics), one
  explicit `this(...)` delegation, and the base `Command` index-2 contract are
  supported. Null, ambiguous, unresolved, and transformed roots remain
  keyless/type-only. Covers FR-1–FR-5.

## Discovery and classification

- Preflight was performed at `e4d3cdee56f2851c28560a9bdc88ab041f296fb3`.
  Before the fixture addition, dummyapp had 14 explicit application-command
  constructors (7 direct-parameter and 7 null) plus one implicit no-argument command.
  Quizzes had 78 application-command constructors: 57 direct-parameter, 5 direct
  getter, and 16 null. Neither application used command-constructor chaining or an
  unsupported root transform. Quizzes therefore requires no general data-flow engine.
- The production visitor order is safe: the complete command-handler phase visits all
  Java files before `WorkflowFunctionalityVisitor`. Constructor metadata is indexed
  independently of handler discovery and keyed by qualified constructor signature, so
  command and handler file order does not matter.
- **Autonomous implementation detail:** `StepDispatchFootprint` carries the canonical
  property suffix from the Saga constructor input to the semantic key. The analysis
  state appends it to `GroovySourceValueReference`, and the adapter preserves it when
  canonicalizing step keys. This is necessary so a command rooted at
  `dto.getAggregateId()` identifies the produced DTO's `aggregateId`, not the whole
  DTO, during later same-source matching. It does not change package shape.
- No material scope delta or plan deviation was found.

## Actual changes

- Added `CommandRootKeyPath` and an order-independent command-constructor metadata
  index in `ApplicationAnalysisState`.
- Extended `CommandHandlerVisitor` to derive bounded root-key paths from application
  command definitions and explicit constructor delegation.
- Replaced the fixed third-argument inference in `WorkflowFunctionalityVisitor` with
  constructor-signature lookup and semantic substitution at each creation site. Its
  explicit source-expression grammar accepts resolved names/fields, direct
  zero-argument instance getter/property chains, and literals; it rejects helper
  calls, transforms, binary expressions, and unresolved expressions.
- Preserved key property paths through `StepDispatchFootprint`, source aggregate-key
  evidence, and adapter step-key canonicalization.
- Added the source-only dummyapp `SemanticRootItemCommand` fixture and its Item handler
  mapping. Its overloads cover reordered/extra ids, a DTO getter, delegation,
  unsigned and signed literals, null, an unsupported transform, and an intentionally
  ambiguous delegation.
- Updated visitor/state/realistic source-trace tests to assert the corrected symbolic
  evidence rather than the former type-only third-argument result.

Changed implementation and fixture files:

- `applications/dummyapp/.../item/commands/SemanticRootItemCommand.java`
- `applications/dummyapp/.../item/commandHandler/ItemCommandHandler.java`
- `verifiers/.../buildingblock/CommandRootKeyPath.java`
- `verifiers/.../buildingblock/StepDispatchFootprint.java`
- `verifiers/.../state/ApplicationAnalysisState.java`
- `verifiers/.../visitor/CommandHandlerVisitor.java`
- `verifiers/.../visitor/WorkflowFunctionalityVisitor.java`
- `verifiers/.../scenario/adapter/ApplicationAnalysisScenarioModelAdapter.java`
- focused Spock specifications for those surfaces and the retained Quizzes
  RemoveTournament/AddParticipant source-derived case.

## Proof

From `verifiers/`:

```text
mvn -q -Dtest=CommandHandlerVisitorSpec,WorkflowFunctionalityVisitorSpec,ApplicationAnalysisStateSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec,GroovyConstructorInputTraceVisitorDummyappSpec,DummyappAccountingFixtureFoundationSpec test
```

Result: 108 tests, 0 failures, 0 errors, 0 skipped.

The consistent reviewer reran the same focused suite after one correction round and
reported no remaining blocking or advisory findings. The correction added an explicit
creation-site expression grammar, rejected helper/transformed/binary/unresolved key
expressions, and recognized signed numeric literals as exact.

The proof includes:

- a fourth-argument root winning over an unrelated id carried earlier by the command;
- direct parameter, getter/property, delegated, command-definition literal, and
  creation-site literal roots, including `-`/`+` numeric literals;
- explicit null, ambiguous delegation, command-definition transforms, and
  creation-site helper transforms remaining keyless;
- a resolved local alias remaining inspectable symbolic evidence without claiming a
  Saga-constructor input origin;
- stable results when command files are visited in reverse order;
- canonical `aggregateId` appended to source producer provenance;
- real Quizzes Tournament footprints changing from type-only to symbolic while the
  retained Remove/Add shared producer still resolves to constructor argument 1;
- the current package reader/writer field contract remaining valid when deterministic
  ordering places a type-only interaction first.

## Exclusions and next milestone

- No Quizzes source, input recipe, acceptance/materializability, interaction selector,
  accounting rule, workload scheduling, package role/shape, dynamic analysis, or
  execution behavior was changed.
- Bare `Command` has a reusable built-in index-2 path, but generic compensation
  dispatch recognition remains M2.
- `SagaCommand` transparency and false limitation cleanup remain M2.
- Canonical current-state/audit updates and the size-1–3 Quizzes comparison remain M3.
- `lib/` and `tmp/` were not touched. No branch or commit was created.

## What to inspect / reasonable veto points

- Inspect the semantic fixture test to see reordered, getter, delegated, exact, and
  unresolved cases side by side.
- A successfully resolved plain variable is now symbolic (`itemAggregateId`) rather
  than type-only, as FR-3 requires.
- Resolved local/field names and direct getter/property chains may remain symbolic
  without a Saga-constructor input index; the verifier does not claim data-flow it has
  not proven. Non-getter helper calls and transformed expressions remain type-only.
- A getter declared inside the command is rendered canonically as
  `itemDto.aggregateId` and the same `aggregateId` suffix is appended to source
  provenance. This canonical form is an intentional internal representation choice.
- The deliberately ambiguous overload exists only in dummyapp's source-only verifier
  fixture; it is not application runtime code.
