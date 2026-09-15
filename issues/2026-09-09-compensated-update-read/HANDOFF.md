# Compensated-update read diagnostic — handoff

State: **complete** for the approved [brief](BRIEF.md).

## Shipped outcome

The existing opt-in Saga/local diagnostic now detects reads of forward updates followed
by explicit compensation that restores at least one changed top-level application
attribute. It retains the compensated-creation category and their shared separate count.
No simulator, Quizzes behavior, ImpactV1, persistent-object ImpactV2 score or search
fitness was changed.

The proof joins the exact delivered revision, its persisted predecessor, the unique
producer action and a later direct-successor recovery write from the same Saga and
explicit source occurrence/checkpoint. It uses existing baseline/write projections and
read-response observations; there is no new persistence query or arbitrary log parser.

Schema `microservices-simulator.saga-read-exposure.v2` adds:

- `applicationAttributeFingerprints` on revisions: canonical JSON SHA-256 per top-level
  projected attribute. Maps have sorted keys; lists retain order. Structured values are
  compared atomically. Raw values are not copied into the sidecar.
- `category: CREATION | UPDATE`, generic produced/recovery revision and write IDs.
- `restoredAttributes` and `notRestoredAttributes` for attributes changed by the forward
  update. They do not claim to inventory every new side effect introduced by recovery.
- Creation-specific compatibility fields remain populated only for creation findings.
  Finding IDs and sidecar schema are versioned; do not compare IDs across v1/v2 as a
  stability promise.

Repeated deliveries to the same reader of the same produced revision share one finding.
Different readers remain distinct. Reader recovery does not erase an already established
exposure. Missing/ambiguous evidence and intervening writers remain unknown; baseline and
recovery-produced reads, metadata-only updates, no restoration and the successful-update
control do not produce update findings. Unknown counts are lower bounds, not safety claims.

## Validation and actual results

Focused Java 21/Spock validation passed:

| Suite | Passed | Failures/errors |
| --- | --- | --- |
| SagaReadExposureSpec | 57 | 0 / 0 |
| ScenarioExecutorSpec | 197 | 0 / 0 |

The generic cases cover full/partial/no restoration, metadata-only effects, missing
predecessor/projection/checkpoint, fingerprint key-set gaps, competing/unattributed writes,
exact revision identity, repeated reads, recovery-produced reads, creation behavior and
collector/executor integration. The implementation is application-independent; the
application-side matrix separately checks actual Quizzes operations and DTOs.

**Eleven fresh JVM runtime executions passed**, using the current detector compiled over
hash-verified unchanged simulator/application classes:

| Control | Executions | Production result |
| --- | --- | --- |
| Update succeeds, B reads between writes | 2 (JSON off/on) | 0 exposures |
| A updates, B reads, A fails and compensates | 2 (JSON off/on) | **1 UPDATE exposure each** |
| B reads before A updates | 2 (JSON off/on) | 0 exposures |
| B reads after A compensates | 2 (JSON off/on) | 0 exposures |
| Existing read-only compensated-creation positive | 1 | 1 CREATION exposure |
| Existing creation failed-read control after compensation | 1 | 0 exposures |
| Ordinary ScenarioExecutor setup-backed FindQuiz control | 1 | SUCCESS; persisted v2 sidecar with 0 exposures |

All eight update diagnostic reports have `COMPLETE_WITHIN_SCOPE` coverage. An independent
application verifier first checks raw DTO/persistent evidence, action order, exact assigned
fault and recovery modes; the integrated verifier then compares that witness with the
production sidecar. All eight agree. The runtime batch took 156.46 s including harness
compilation/startup; it is not a diagnostic-overhead measurement.

The positive identifies Tournament **11/v21**, read by B, and recovery **v22**. It lists
`endTime`, `numberOfQuestions` and `startTime` as restored, and `lastModifiedTime` plus
`tournamentTopics` as not restored. The real dates were 15:00 → 16:00 → 15:00, and B
committed retaining 16:00. No later business use or domain harm was inferred.

## Actual change inventory

Production verifier files under `verifiers/src/main/java/.../faults/executor/`:

- `SagaReadExposureAssessor.java`: shared creation/update dispatch, update proof and dedup.
- `SagaReadExposureReport.java`: v2 evidence/category/restoration contract.
- New `SagaReadAttributeFingerprinter.java`: deterministic attribute equality evidence.

`SagaReadExposureCollector`'s public API and its ordinary-executor composition are unchanged;
it obtains fingerprints through the revised snapshot-to-revision conversion.

Verification and documentation:

- `SagaReadExposureSpec.groovy`: generic positive/negative/unknown regressions.
- `verifiers/experiments/saga-update-read/SagaUpdateReadExperiment.java`: optional production
  collector integration with a source contract declared before measurement and actual
  action outcomes; existing research-only mode remains available.
- New integrated runner/validator and shell script in that experiment directory.
- Current-state/glossary, roadmap, research synthesis and experiment README now distinguish
  implemented behavior from the original experiment and pending search qualification.

The inherited meeting-note changes and personal `note-04-09-2026.md` were preserved.
No branch/worktree, push, PR or merge was created.

## Review, build discoveries and evidence

Sol/medium implemented the generic change; Astra integrated the runtime and reviewed the
production diff and actual evidence. Pre-review tightened missing projection handling,
recovery-versus-ordinary-write attribution and later writes with unproven authorship so
missing evidence cannot silently become a clean negative. No blocking finding remains
within the approved bounded rule.

The initial local build encountered an unsuitable installed simulator artifact/generated
protobuf build path. Focused tests passed with Java 21 using the private verified simulator
repository (`/tmp/saga-read-m2.zHF7pU/repository`). No simulator source repair was needed.
Runtime qualification reused the retained build only after verifying every unchanged
production file and all 1,777 recorded build/dependency artifacts, then compiled/froze the
four current diagnostic Java files ahead of the old classes. The runner rechecked source
and prepared artifacts after execution. This avoids claiming that the old detector was
used to qualify the new behavior.

- [Qualification matrix](qualification.json)
- [Actual positive v2 sidecar](update-witness.json)
- [Provenance, source hashes and test counts](proof.json)
- [SagaReadExposureSpec result](SagaReadExposureSpec.txt)
- [ScenarioExecutorSpec result](ScenarioExecutorSpec.txt)
- Raw reports/logs: `verifiers/target/saga-update-read/integrated-01/`
- [Reproduction](../../verifiers/experiments/saga-update-read/README.md#integrated-diagnostic-qualification)

## Remaining work, in priority order

1. Qualify a normal workload/scenario with the positive read/update ordering and use it
   in search evaluation. In the retained 7 September catalogue, all nine exact
   FindTournament/UpdateTournament pairs place the read first. The
   [inventory](retained-workload-check.json) is a bounded observation of that package,
   not a claim that every possible generated workload has this order. The controlled
   positive here is not a generated positive; ordinary integration is separately proven
   by executor tests and the real baseline sidecar control.
2. Fix the separate Tournament topic course-ID loss with an application regression test
   when selected. It was deliberately preserved here to qualify partial restoration.
3. Put the measured history and structured-instrumentation boundary into the paper, and
   advance GA qualification. Decide explicitly how the anomaly count guides search;
   do not silently add it to the persistent-object count. New fingerprinting cost has
   not been isolated; earlier v1 memory/cost figures do not measure this extension.

Internal reads, query predicates/lists, nested response references, event consumers,
arbitrary DTO field use, other transports and other anomaly families remain outside
scope. Multi-writer histories and multiple later same-Saga recovery candidates are
conservatively unknown. This work is a useful bounded detector, not a serializability
oracle or a claim to detect every effect of compensation.
