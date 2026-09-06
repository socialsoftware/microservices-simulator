# Complete source-derived setup

## What complete source-derived setup is

The verifier can currently turn supported calls from a Spock `setup()` fixture into an
ordered SetupPlan and bind retained results to Saga participant arguments. The latest
complete Quizzes single-input analysis classifies 560 of 796 accepted inputs as static
setup candidates. The remaining 236 are three rejected setup plans, 175 inputs with only
some required setup-result bindings, and 58 inputs with no binding.

Many of those inputs obtain a required value from an application-facade call made in the
same feature before the target Saga call. The visitor already observes such calls, but the
adapter currently builds candidates only from calls whose context is `setup`. This change
extends source-derived setup to the supported, source-ordered prefix of the same observed
feature execution. It also represents the observed two-property result path
`quiz.aggregateId` without turning setup execution into an open property interpreter.

An **observed setup context** for this feature is one source class, one exact Spock feature
method, its ordinary per-feature `setup()` actions, and the supported application-facade
calls that occur before one exact target occurrence in that feature. Separate feature
methods remain separate test executions even when they belong to the same class.

## Goals

1. Bind participant arguments from supported application-facade results created before
   the exact target occurrence in the same observed feature execution.
2. Preserve the exact occurrence identity, order, and void effects of the retained setup
   prefix.
3. Prevent a selected target participant occurrence, a post-target call, an assertion
   read, or a different feature's actions from being replayed as setup.
4. Keep multi-participant setup truthful by requiring one common observed feature context
   and a prefix that does not contain any selected participant occurrence.
5. Support the concrete `quiz.aggregateId` result path through the existing persisted
   result-property field and a closed, type-checked DTO path contract.
6. Measure the actual Quizzes gain without assuming that all 236 blocked inputs become
   candidates or setup-ready.

## Non-goals

- Combining setup or inputs from separate feature methods, test classes, or dynamic test
  executions.
- Replaying calls after the target, assertion/query calls used only to inspect the result,
  cleanup methods, or earlier unrelated features.
- Treating arbitrary Saga calls, arbitrary getters, or arbitrary dotted property strings
  as executable setup.
- Resolving indistinguishable repeated target occurrences by source text, line parsing,
  or first-match selection.
- Adding general Groovy control-flow interpretation, branch/loop execution, setup-spec
  lifecycle emulation, event-subscriber prerequisites, or cross-test setup synthesis.
- Changing input selection, scheduling, faults, recovery, ImpactV1, dynamic evidence,
  the GA/search work, or Quizzes production behavior.
- Guaranteeing that all statically accepted candidates pass runtime preflight.

## Functional requirements

### Exact context and cutoff

**FR-1.** Every direct-feature input considered for feature-derived setup shall retain an
exact internal target occurrence identity. The association shall come from the typed AST
trace, not reconstructed expression text or a parsed occurrence string.

**FR-2.** A candidate for a target occurrence shall contain the supported `setup()`
actions followed by supported actions from the same feature that occur strictly before
that target. Source order shall be preserved across both phases.

**FR-3.** The target occurrence itself and every supported action after it shall be absent
from that candidate.

**FR-4.** Calls from another feature method, another source class, cleanup, or an
unrelated test execution shall never enter the candidate.

**FR-5.** A prior, distinct facade call in the same feature may be retained when it is in
the observed prefix. This includes a prior Saga-facing call whose effect establishes the
precondition for the later target, provided it is not one of the selected participant
occurrences.

**FR-6.** Retained void effects shall remain in their exact observed positions. A value
producer and its related void effects shall not be duplicated when several bindings use
the same result.

**FR-7.** The admitted feature prefix is only the initial straight-line preparation
phase. Encountering `then:`, `expect:`, `cleanup:`, `where:`, control flow, or direct
workflow/event-handler execution permanently closes prefix extension for later calls in that feature.
Conditional, repeated, ambiguous, or otherwise unsupported source shapes shall remain
blocked with a stable diagnostic rather than being flattened speculatively.

### Binding and participant composition

**FR-8.** A participant argument may bind only to a retained, earlier action result or an
approved result property from its observed context. Existing independently materializable
arguments continue to need no setup binding.

**FR-9.** A setup candidate shall cover an input only when every setup-dependent argument
of that input is supplied completely and unambiguously.

**FR-10.** A multi-participant workload may use feature-derived setup only when its inputs
share at least one exact feature owner and one candidate for that owner completely covers
the selected tuple. The candidate frontier shall be the earliest selected target's exact
occurrence. Any unselected facade effect between selected targets blocks the tuple; it is
not omitted or hoisted before the frontier.

**FR-11.** No selected participant occurrence may also appear as a SetupAction. If a
selected participant's result is required to materialize another selected participant,
the tuple remains blocked because all participants must be materialized before measured
execution begins.

**FR-12.** Identical candidate semantics may be deduplicated deterministically. If
several occurrence/context candidates cover the same tuple but are not semantically
identical, setup attachment shall remain ambiguous and blocked.

**FR-13.** When current InputVariant identity collapses multiple target occurrences whose
safe prefixes differ, the verifier shall reject feature-derived setup for that input
rather than choose an occurrence heuristically. Changing persisted input identity is not
part of this iteration.

### Bounded nested result paths

**FR-14.** The current `resultProperty` representation shall accept the exact initial
path `quiz.aggregateId` only when the retained action's declared result type satisfies
the existing DTO-root contract. Before any setup action is dispatched, runtime contract
validation shall require that root's declared public zero-argument `getQuiz()` return
a DTO type and that intermediate type's declared public zero-argument `getAggregateId()`
return an Integer-compatible type.

**FR-15.** Existing one-property `aggregateId` and `courseAggregateId` behavior shall
remain unchanged. Non-DTO roots, missing or non-public getters, wrong intermediate or
leaf types, unknown or blank segments, longer paths, and every other dotted path shall be
rejected before setup dispatch.

**FR-16.** Runtime property access shall use only exact zero-argument getters for a path
already accepted by static validation. This change shall not add field access, arbitrary
method invocation, fallback lookup, or broaden the closed facade-dispatch map.

### Determinism and evidence

**FR-17.** Action order, action identity, binding order, setup identity, workload
identity, diagnostics, and package bytes shall remain deterministic for identical input.

**FR-18.** `setups.jsonl`, the WorkloadPlan setup reference, and the structural shape of
`resultProperty` shall remain unchanged. The existing `property` string carries the one
new allowlisted path value.

**FR-19.** Focused dummyapp tests shall prove positive and negative cutoff, context,
selected-occurrence, ambiguity, multi-participant, void-effect, and nested-path behavior
before Quizzes qualification.

**FR-20.** Quizzes qualification shall report before/after single-input categories,
concrete newly covered examples, and a bounded preflight sample. It shall distinguish
static setup candidacy from runtime setup readiness and shall not extrapolate to all 236
current blockers.

## Architecture

The existing pipeline remains:

```text
typed Groovy visitor facts
  -> ApplicationAnalysisState
  -> context/cutoff-aware setup candidates
  -> exact selected-tuple setup attachment
  -> existing setup/workload artifacts
  -> validator and ScenarioExecutor
```

The visitor must expose an internal, typed relationship between an input trace and its
exact source occurrence, plus enough ordered context to compare that occurrence with
candidate setup actions. The adapter builds candidates at target cutoffs using `setup()`
actions and one feature-local prefix. Candidate metadata retains its feature owner and
the target occurrences excluded from replay. The generator attaches a candidate only
after checking common ownership, complete bindings, selected-occurrence exclusion, and
unambiguous semantics.

This is not dependency slicing: within the admitted prefix, all currently supported
Java-confirmed facade actions are retained so that void prerequisites are not lost. The
cutoff itself prevents post-target assertion/query calls from entering setup. Existing
validator and executor boundaries remain authoritative.

The nested-property extension reuses the existing static DTO-root authority and admits
only the exact path `quiz.aggregateId`. Pre-dispatch runtime contract validation proves
the declared getter signatures and their intermediate/final types before any setup action
runs. A structurally compatible DTO from another application may satisfy the same
contract; generic verifier code must not name a Quizzes or fixture FQN. The extension
reuses the current `resultProperty` field and does not authorize generic reflection or
new facade methods.

## Data model

The visitor/state layer gains internal occurrence/context data sufficient to identify:

- source class and exact call-context feature;
- exact target occurrence;
- deterministic order within that context;
- the corresponding setup-capable facade occurrence, when present.

`SourceSetupPlanBinding` may gain internal observed-context and excluded-target metadata
so the generator can enforce FR-10 through FR-13. These fields are analysis-time data and
are not added to the package.

The persisted SetupPlan remains an ordered list of actions and participant bindings.
`SetupValueRecipe.propertyName` remains the serialized `property` string. For the bounded
nested case it contains `quiz.aggregateId`; static validation and runtime execution treat
that value as one exact allowlisted path, not as unrestricted syntax.

Runtime action results and getter values remain attempt-local and outside deterministic
package identity except through their source recipe.

## Security model

The application-owned closed setup dispatcher remains unchanged. Only Java-confirmed
facade method keys may execute. The nested path adds no general object inspection: static
validation admits only a DTO root and the exact `quiz.aggregateId` token, while runtime
contract validation proves the declared public zero-argument getters return a DTO then an
Integer-compatible type before dispatch. The runner invokes only that already validated
chain. No application or fixture FQN, field access, setter, method-name fallback,
classpath scan, or arbitrary reflective traversal is allowed.

## Operating

No CLI option or configuration key is added. Rollout is the normal current-package
generation path after focused tests and bounded Quizzes qualification. Existing packages
remain readable because record shapes do not change; newly generated packages may contain
the newly admitted `quiz.aggregateId` property value.

Rollback is a normal revert of the occurrence/prefix candidate logic and bounded path
support. The prior `setup()`-only behavior remains the comparison baseline.

Canonical current-state, roadmap, and the ordered-setup ADR must be updated during
implementation to describe the shipped context/cutoff rule and exact path allowlist.

## Future roadmap

A future issue may give every repeated source occurrence its own persisted InputVariant
identity if that is required to represent several semantically distinct targets that are
currently collapsed. Setup-spec/class lifecycle support, event-consumer prerequisites,
and generated cross-test setup remain separate decisions.

## Resolved implementation decisions

The defaults below were selected under the user's autonomous execution instruction.

| # | Decision | Default | Alternatives | Impact |
|---|---|---|---|---|
| 1 | How should source ordering and target cutoff be represented internally? | Add typed occurrence/context ordering emitted by the visitor | Parse the existing occurrence string or match source text | Typed data avoids unstable text/line heuristics and makes selected-occurrence exclusion testable. |
| 2 | What prefix is retained? | All currently supported, Java-confirmed facade actions before the exact target in the same feature, after ordinary `setup()` | Only transitive value producers | Whole-prefix retention preserves observed void/state prerequisites; the strict cutoff limits accidental replay. |
| 3 | How should collapsed repeated target occurrences be handled? | Block when their safe prefixes differ | Change InputVariant identity now; select first occurrence | Blocking preserves truthfulness without reopening the persisted input contract. |
| 4 | How is `quiz.aggregateId` represented? | Keep `resultProperty.property` and admit that exact dotted value for any root satisfying the existing DTO contract and declared getter-type checks | Add a path array/schema revision; hardcode an application FQN; allow arbitrary dotted traversal | The default is application-independent, is the smallest compatible extension, and keeps the security boundary closed. |
| 5 | What numerical result is required? | Report the measured gain and unresolved categories; no minimum count | Require all 236 to become candidates | The remaining set includes distinct gaps and ambiguity, so a headline target would reward unsafe inference. |
