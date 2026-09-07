# M0 handoff — exact response delivery

State: **complete**. Independent review passed on its second pass; M1 may proceed.

## Outcome and scope

Implemented the framework delivery boundary for FR-1, FR-2, FR-6 and FR-7. The opt-in
hook observes the actual final successful return of `LocalCommandGateway.send`, after
deserialization, UoW merge and exception handling. Typed adapter contracts declare exact
command/response classes, logical and runtime persistent types, and outer identity/revision
extraction. No persistence read, DTO mutation, naming heuristic or payload storage was added.

Only `DELIVERED` carries admissible provenance. Unmapped/invalid returns, failed calls and
exclusions retain explicit reasons. Attribution must remain the same complete forward Saga
action across the call. The exact framework `SagaCommand` is unwrapped. Setup, probes,
recovery, event consumers and observer callbacks cannot become application readers.

## Actual files

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/local/LocalCommandGateway.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/ImpactEvidenceObserver.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/ImpactEvidenceObserverHolder.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/sagaread/ReadResponseAdapter.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/sagaread/ReadResponseEvidence.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/sagaread/ReadResponseObservation.java`
- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/sagaread/ReadObservationContext.java`
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/local/LocalCommandGatewayReadResponseTest.java`
- `applications/dummyapp/src/main/java/com/example/dummyapp/diagnostics/ReadResponseFixture.java`
- `docs/verifiers-impl/current-state.md` and this handoff.

## Discovery and decisions

Read the actual gateway return/error paths and observer holder before implementation.
The dummyapp fixture is compiled by the gateway test and remains source-only. Read
callback failures use a separate holder queue, leaving ImpactV2 gap ownership intact.
Review found that legacy observer callbacks could issue recursively attributed reads,
and that the real `EVENT_CONSUMER` role and failed excluded calls needed explicit treatment.
Both root causes and adjacent cases were corrected and tested. `FAILED_INVALID` distinguishes
a failed call with untrustworthy attribution. These are necessary in-scope details; no
material scope change or plan deviation occurred.

## Proof

JDK21, isolated source and independent Maven repository at
`/tmp/saga-read-exposure-28go53x8`; no shared targets/cache or Docker used. Command from
`source/simulator`:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home mvn -q -Dmaven.repo.local=/tmp/saga-read-exposure-28go53x8/maven-repository -Dtest=LocalCommandGatewayReadResponseTest,LocalCommandGatewayDynamicEvidenceTest,ImpactEvidenceObserverHolderTest,CommandGatewayExceptionRestorationTest test
```

**28 tests passed**, zero failures/errors/skips (18 new gateway tests, 10 regressions).
Direct/JSON delivery, returned versus internal revision, exact typed contracts, retries,
semantic-lock failure after DTO construction, invalid attribution, excluded roles,
callback failure and recursion are covered. Raw Maven log, Surefire XML, test summary and
source manifest: `/tmp/saga-read-exposure-28go53x8/reports/m0/`.
Manifest SHA-256: `55978960f6c20e5f318a086d4ee40ce8e459ba52792799505135cf0737c4acf6`.
Reviewer checked all 169 measured files against both snapshot and checkout; orchestrator
checked changed-source equality, XML totals and actual diff. `git diff --check` passed.

## Next milestone and user flow

The hook has no production assessor or sidecar yet. M1 must own one combined diagnostic
write/read sequence, exact action/checkpoint joins and persistence; it must consume read
failures separately and exclude persistent observation/probe work. M2 adds audited Quizzes
adapters and fresh runtime proof. No user decision is pending. To inspect M0 alone, run the
focused gateway suite in an isolated module snapshot with JDK21 as above.
