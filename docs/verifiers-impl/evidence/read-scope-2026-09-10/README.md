# Read command scope before reader attribution

The six false partial-coverage results are corrected. **All 14 repeated source-derived
Tournament update/read histories now have complete read coverage within the declared
scope.** The six controls still have I=0/A=0; the eight faults still have I=1, with
three A=1 and five A=0. Application and search scoring rules are unchanged.

## Correction

`ReadResponseObservation` used to require forward-reader attribution before selecting
the exact command adapter scope. A successful `CommitSagaCommand` has no read adapter
and no forward-reader context; the earlier observer reported it as an invalid delivery.
The observer now applies role exclusions and checks payload/command scope before reader
attribution. Successful unmapped calls remain `DELIVERED_UNMAPPED/NO_READ_ADAPTER`.
Failed unmapped calls are excluded with `COMMAND_OUTSIDE_DECLARED_SCOPE`; the gateway
continues throwing the original application exception. Failed scope inspection is
contained as diagnostic evidence and cannot replace that exception.

Supported reads still require the same attribution, exact response type, adapter metadata,
identity and revision. Missing payload and adapter errors remain invalid. Setup, recovery,
event and observer exclusions keep their precedence. No command-name special case or
new Quizzes adapter was introduced.

The only production file changed is simulator `ReadResponseObservation.java`. Gateway
tests cover unmapped successes/failures without reader attribution, malformed payload,
throwing adapter scope and missing/changed attribution on supported reads. Both direct
and JSON transport modes are exercised. The existing source-only dummyapp revision fixture
and the gateway dynamic-evidence/exception-restoration tests also pass.

## Verification and retained evidence

- [Tests](tests.json): the final pre-fix run reproduced four assertion failures among
  23 read-response tests. After correction, all 31 tests in the three gateway suites pass.
  Earlier tooling/fixture attempts are recorded there, separately from that red/green proof.
- [Runtime validation](validation.json): 14 fresh JVM/Spring/H2 executions in Docker,
  fixed application clock, Saga/local JSON transport, all EXACT and all complete within scope.
- [Comparison](comparison.json): 42 equivalent execution/ImpactV1/ImpactV2 report pairs;
  all read findings unchanged; exactly six call observations change from invalid attribution
  to unmapped command. The generated package files are byte-identical to the baseline.
- [Provenance](provenance.json), [generation exit](generation-exit.json),
  [execution exit](execution-exit.json) and [proof](proof.json): hashes of the explicit
  source overlay, retained build, tests, comparison code and artifacts.
- [Updated discussion matrix](../impact-anomaly-matrix-2026-09-10/README.md) and
  [previous matrix](../impact-anomaly-matrix-2026-09-10/matrix-before-read-scope.json).

Comparison normalizes the exact execution-attempt ID, source-setup duration and the
setup binding's default `TournamentDto@hex` Object.toString value. It preserves application
projections, revisions, action outcomes, fault assignments and both impact reports.
Finding IDs incorporate the attempt; their evidence fields are compared independently.
Scope remains the two declared outer Quiz/Tournament read contracts.

Raw reports are in `verifiers/target/saga-update-read/read-scope-01/`; originals remain in
`source-input-02/`. Unit-test logs/XML are in `verifiers/target/read-scope-tests-2026-09-10/`.
The [existing harness instructions](../../../../verifiers/experiments/saga-update-read/source-inputs/README.md)
include the new overlay and comparison command. The runner now accepts explicit simulator
sources alongside verifier sources; the validator requires complete coverage for all
14 current histories. Earlier runs retain their frozen validators and original reports.

## Remaining work

The Quizzes topic course-ID compensation defect is unchanged and remains the source of
I=1 in the eight fault histories. The combined search formula is still undecided; discuss
the examples in the Portuguese meeting note before choosing weights. Broader anomaly
investigation and GA preparation remain separate roadmap work.
