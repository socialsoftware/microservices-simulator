# M1 handoff — auditable diagnostic and executor sidecar

State: **complete**. Independent review passed on its second pass. M2 may proceed.

## Outcome

FR-3–FR-8 are implemented through the generic `SagaReadExposureCollector`, pure assessor
and metadata-only report. The ordinary executor composes the diagnostic with its existing
ImpactV2 observer. One diagnostic sequence orders copied confirmed writes and actual
deliveries; ImpactV2 retains its original write/event sequence and separate read-independent
gaps. No additional persistence query or application payload copy was added.

The rule proves covered prior absence, a unique creation, exact delivery to another Saga
before producer success, and direct-predecessor deletion by explicit compensation of the
same producing occurrence/checkpoint. Unknown attribution, intermediate revisions, restored
updates and missing compensation proof remain unknown. Reader writes or harm are not
required. Repeated deliveries share one finding per producer/reader/compensated revision;
different readers remain distinct. Proven positives survive reader recovery, later
recreation and incomplete prefixes, with partial coverage retained.

The opt-in flag resolves from the same Spring environment as the gateway, with a JVM-property
fallback for non-Spring fixtures. Enabled attempts write `<execution>.saga-read-exposure.json`;
disabled attempts create no new sidecar. Disabled/unavailable write observation or failed
observer installation yields `UNAVAILABLE` with null count. The report carries artifact
hashes, exact source contracts, actions, facts, verdicts and scope without an impact score.

## Actual files

Under `verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/`:

- New `SagaReadExposureReport.java`, `SagaReadExposureCollector.java`, `SagaReadExposureAssessor.java`.
- Updated `ImpactV2EvidenceCollector.java` and `ScenarioExecutor.java`.

Tests:

- New `verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/SagaReadExposureSpec.groovy`.
- Updated adjacent `ScenarioExecutorSpec.groovy`.
- Updated `verifiers/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/FixtureWorkflow.java`.

Documentation: `docs/verifiers-impl/current-state.md`, `docs/verifiers-impl/roadmap.md`, this
handoff. The separately committed `M2-PROTOCOL.md` fixes runtime proof before measurements.

## Discovery and review

Read the current recovery/source joins, observer installation and output guards before
implementation. Runtime fallback chooses a source by name; the assessor accepts it only
when actual source evidence is unique. The current package validator already guarantees
one checkpoint per scheduled source step; no catalogue contract change was necessary.
`FixtureWorkflow` gained a resettable optional test-body callback to emit evidence under
real executor attribution. M0 retains responsibility for proving the real gateway boundary.

Review found and resolved three in-scope defects:

1. The existing package-output guard protected only a subset of manifest roles. It now
   enumerates every manifest-declared artifact, including custom paths and optional dynamic
   artifacts, and rejects symlink/hardlink aliases before execution.
2. A proven ordinary forward deletion after A succeeds following B's read now gives
   `NOT_OBSERVED`; missing or mismatched deletion authors remain `UNKNOWN`.
3. An occupied observer holder now makes the diagnostic unavailable with null count while
   preserving the executor's existing replay and ImpactV2 handling.

Malformed/unreadable manifests already fail in the reader before output guards; the shared
guard correction does not introduce a new selection failure mode. These changes are
necessary implementation details inside the approved boundary, with no material scope delta.

## Proof

JDK21, isolated source and independent Maven repository at `/tmp/saga-read-exposure-28go53x8`.
The current M0 simulator was installed only into that private repository. From `source/verifiers`:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn -q -Dmaven.repo.local=/tmp/saga-read-exposure-28go53x8/maven-repository -Dtest=SagaReadExposureSpec,ScenarioExecutorSpec,ImpactV2AssessorSpec test
```

Final Maven exit 0: **247 tests passed**, zero failures/errors/skips: 42 diagnostic,
195 executor, 10 ImpactV2 assessor. Coverage includes the positive/negative/unknown matrix,
deduplication, two readers, old-attempt rejection, prefix retention, adapter ambiguity,
observer failure isolation, no duplicate snapshot queries, Spring precedence, ordinary
executor persistence/hash evidence, on/off outcome/ImpactV1/ImpactV2 equivalence and output
alias/write-failure controls. The invariant signal remains nonzero in the on/off executor
control, avoiding a vacuous ImpactV1 comparison.

Final raw log, XML, summary and manifest: `/tmp/saga-read-exposure-28go53x8/reports/m1/`.
The initial 230-test proof is retained separately at `reports/m1-pass1/`.
Final source manifest SHA-256:
`1458823ecedcd9afdaf4634ba0985e1917dcebf22e479ff90a41ee950f42a822`.
Reviewer and orchestrator independently checked all 494 entries against both checkout and
snapshot and parsed the final XML totals. `git diff --check` passed. Isolated simulator JAR
SHA-256: `b6eed27824c96db0afc4a84e358169cac2d95b231c4227693d673745fcf65489`.
No shared cache/targets or Docker were used.

## M2 seam, limitations and user flow

Construct the public diagnostic with attempt/workload/scenario IDs and `SourceContract`;
pass it to the package-private four-argument `ImpactV2EvidenceCollector`. Its `start` uses
the existing baseline and adapter beans; `finish` and `recordObserverFailures` preserve
the separation. `diagnostic.report(execution, artifactReferences)` is the same assessor
used by production. The experiment may compile into the executor package to reuse this
composition without adding a public API just for the harness.

Identity/revision lookup is indexed, but per-call object-history and fallback-action scans
can cost `O(R * W_same_identity + R * A)`. Retention has no cap; do not claim globally linear
assessment or a measured cost until M2. The collected graph contains metadata only.

Quizzes adapters, new Docker/runtime evidence, cost and practical application commands
remain M2 obligations. Existing experiments are not new proof. To inspect the generic
slice now, run the focused suites above in an isolated snapshot; an application needs
explicit adapter beans before its enabled report has usable scope. No user decision is
pending and no normal generated runtime result binding has been added.
