# M2 handoff — transparent wrappers and useful limitations

- **State:** complete; milestone review passed on pass 2.
- **Outcome:** `SagaCommand` is transparent to static interaction extraction, the
  existing bare-`Command` semantic-lock compensation form produces a compensation
  footprint when its service and key are statically available, and step limitations
  now describe only command/target/key shapes that remain unresolved. Covers
  FR-13–FR-17.

## Discovery and classification

- The M1 Quizzes package had exactly 134 forward-incomplete steps. Forty-two contained
  typed `SagaCommand` wrapper false negatives. The remaining 92 contained only
  ordinary/framework plumbing: 86 getter/setter/collection/configuration cases and six
  direct typed-command async/complex cases whose gateway/continuation calls were
  falsely flagged. There was no non-wrapper unresolved application command in the
  retained Quizzes source.
- Quizzes contains 68 `SagaCommand` creations: 42 typed forward wrappers and 26 bare
  `Command` compensation wrappers. Every bare form is registered compensation with
  `Command(unitOfWork, ServiceMapping.<service>.getServiceName(), <root>)`, a semantic
  lock, and a gateway send. Roots are direct locals/fields or DTO getter expressions.
- The service token is not always the aggregate name (`ANSWER` routes through
  `AnswerCommandHandler` to aggregate `QuizAnswer`). Generic compensation resolution
  therefore follows the runtime handler naming contract and uses that handler's
  already-extracted unique aggregate, rather than guessing from capitalization.
- The final realistic source assertion retains only two genuinely unsupported Quizzes
  shapes: a conditionally assigned `SagaCommand` payload in
  `AddParticipantAsyncFunctionalitySagas`, and a separately assigned command used by a
  conditional gateway send in `UpdateTournamentFunctionalitySagas`.
- No material scope delta or plan deviation was found.

## Actual changes and autonomous details

- `WorkflowFunctionalityVisitor` now:
  - excludes `SagaCommand` itself from application-command lookup and resolves its
    single payload creation or variable as transparent dispatch plumbing;
  - records the typed payload exactly once and recognizes both `send` and `sendAsync`;
  - resolves compensation-only bare `Command` targets through the exact service-token
    handler and reuses the M0 base-command root-key contract, emitting one concise
    compensation limitation when either target or key is unavailable;
  - replaces blanket method-call diagnostics with semantic command-typed/gateway-call
    checks, bounded source-helper inspection for concealed dispatches, and exact root
    argument localization for unsupported key expressions;
  - treats ordinary source/library operations as irrelevant when they contain no
    command construction or command-typed invocation. Getter-only key chains are not
    limitations; a getter-shaped helper that actually dispatches still is.
- `SagaStepBuildingBlock` deduplicates limitations by phase and code, so repeated
  occurrences do not flood a step.
- Visitor, adapter, realistic source-trace, and current artifact-contract tests cover
  wrapped typed access once, generic compensation target/key, one unresolved payload
  limitation, ordinary calls, an opaque helper, stable recovery generation, and the
  unchanged package surface.

M2 files changed:

- `verifiers/src/main/java/.../buildingblock/SagaStepBuildingBlock.java`
- `verifiers/src/main/java/.../visitor/WorkflowFunctionalityVisitor.java`
- `verifiers/src/test/groovy/.../visitor/WorkflowFunctionalityVisitorSpec.groovy`
- `verifiers/src/test/groovy/.../scenario/adapter/ApplicationAnalysisScenarioModelAdapterSpec.groovy`
- `verifiers/src/test/groovy/.../visitor/SourceDerivedSharedSagaWorkloadAnalysisSpec.groovy`

No Quizzes source, runtime/dynamic/execution code, materializability, input recipes,
schedule/fault/recovery algorithms, Impact/GA behavior, or package role/field names
changed. Canonical current-state and metric updates remain M3.

## Proof

From `verifiers/`:

```text
mvn -q -Dtest=WorkflowFunctionalityVisitorSpec,ApplicationAnalysisScenarioModelAdapterSpec,SourceDerivedSharedSagaWorkloadAnalysisSpec,DummyappAccountingFixtureFoundationSpec,ScenarioGeneratorSpec,StaticAnalysisArtifactWriterContractSpec,CurrentExecutableArtifactContractSpec test
mvn -q -DskipTests compile
git diff --check
```

Result: 132 tests passed with 0 failures, errors, or skips; compilation and whitespace
validation passed. The retained source-derived recovery test still finds the same
immediate `00100` compensation schedule, while its new full-source assertions prove
26 generic compensation footprints and exactly the two limitations listed above.

After the first review requested stronger realistic proof, the Quizzes test was
tightened to assert the exact 26 `(step, aggregate, root expression)` signatures and
symbolic key confidence, including `ANSWER` through `AnswerCommandHandler` to
`QuizAnswer`. The continuing reviewer reran that proof on pass 2 and reported no
remaining findings.

The M1-equivalent count-only Quizzes command was also rerun successfully. Its final
package is under
`verifiers/target/m2-final-profile/quizzes-20260902-181223-170/`: 132 forward command
accesses are present, and only two steps have analysis limitations—the two unsupported
shapes listed above. No getter, setter, collection, wrapper-configuration, recognized
gateway, or compensation limitation remains.

### Review correction pass 1

- The reviewer found a proof gap, not an implementation defect: the realistic test
  counted 26 generic compensation footprints but did not prove each extracted target
  aggregate and root key.
- `SourceDerivedSharedSagaWorkloadAnalysisSpec` now compares the complete stable set of
  all 26 `(Saga step, aggregate, root expression)` signatures against the unmodified
  Quizzes source and also requires every root to retain symbolic key confidence. This
  assertion would fail if target aggregates or keys were absent, duplicated, or
  uniformly guessed.
- The non-identical mapping is explicit proof: the source uses service token `ANSWER`,
  the parsed `AnswerCommandHandler` declares aggregate `QuizAnswer`, and the resulting
  signature is
  `ConcludeQuizFunctionalitySagas::getQuizAnswerStep|QuizAnswer|this.quizAnswer.getAggregateId()`.
- Narrow re-review proof from `verifiers/`:

```text
mvn -q -Dtest=SourceDerivedSharedSagaWorkloadAnalysisSpec,WorkflowFunctionalityVisitorSpec test
git diff --check
```

Result: 34 tests passed with 0 failures, errors, or skips; whitespace validation
passed. The correction changed tests and this handoff only, so production compilation
was not required again; the preceding production compile remains green.

## Exclusions and reasonable veto points

- Wrapper payload tracing is intentionally local: direct construction or a resolved
  local variable initialized by construction. Conditional/reassigned payload flow
  remains limited; no general Java data-flow engine was added.
- Generic compensation service resolution intentionally requires one matching parsed
  `<service>CommandHandler` with one extracted aggregate and a statically supported
  base-command key. Ambiguity produces no guessed access.
- Source-helper inspection is bounded to resolvable source method bodies and follows
  nested source calls only to decide whether a helper may conceal a dispatch. It does
  not infer the helper's command target or inline its data flow.
- `lib/` and `tmp/` were preserved. No branch, commit, push, merge, deployment, or
  release action was performed.
