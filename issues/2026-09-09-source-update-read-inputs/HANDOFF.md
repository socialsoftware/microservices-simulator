# Source-derived Tournament update/read inputs

The existing `UpdateTournamentTest#update tournament successfully` now supplies the
preparation for FindTournament + UpdateTournament through source extraction, current
package export/read and ordinary ScenarioExecutor. The Quizzes test, application,
simulator, anomaly detector and impact policies were not changed by this package.

## What was missing and what changed

The test creates a Tournament, takes the returned DTO, changes its dates and question
count, then passes it to UpdateTournament together with three Topic IDs. Previously the
mapper rejected those IDs inside the set. Simply allowing them would have lost the DTO's
post-creation setters and replayed its initial two-question values.

The visitor now retains supported pre-call setters/property assignments on exact
source-result DTOs. SetupValueRecipe ACTION_RESULT carries ordered assignments; the
writer persists optional result `fields`, and the reader reconstructs them. The validator
uses the existing DTO/property authority and validates references in assignment values.
The mapper also admits approved nested aggregateId projections while preserving exact
producer occurrence/method checks. It never guesses an ID or reruns a selected target as
preparation.

ScenarioSetupRunner copies a mutated result into a no-argument bean through public
getters/setters before applying the supported ordered changes. An unchanged retained
result remains available to other bindings. This is a shallow copy, not general alias
analysis or arbitrary object cloning. Nested in-place mutations, repeated/unknown setters,
unsupported properties/types, missing producers and ambiguous identities remain outside
the supported contract. A projected property's own prior assignment is rejected rather
than silently replaced by the producer's original value; unrelated setters still permit
aggregateId projection. Present non-object result `fields` are rejected by the reader.

## Concrete source-backed experiment

The experiment selects the existing successful source method before outcomes. It does
not substitute provider inputs. The normal generator creates six forward orders for
one FindTournament step and UpdateTournament's five ordered steps, then 55 eager fault
scenarios. The existing selection rule executes 14 of these: all six no-fault controls
and eight schedules with a pre-body fault at the final updateQuizStep. Three placements
of recovery explain why six forward orders produce eight selected fault schedules.

Each execution has a fresh JVM/Spring/H2 database, fixed clock and local JSON transport.
The 14 source-setup facade calls prepare the course, users, topics, questions and
Tournament. At the fixed 2030-01-01T12:00 clock, the baseline has two questions and dates
12:05–13:05. The persisted forward update must show three questions, dates 12:25–13:25,
and three Topics—the actual existing test's values, not qualification-provider values.

| Selected executions | Observation | Compensated-update read findings | Persistent-object ImpactV2 |
|---|---|---:|---:|
| Six no-fault controls | Both Sagas complete successfully | 0 each | 0 each |
| Three fault cases reading before the update | Reader receives the previous version | 0 each | 1 each |
| Three fault cases reading after update, before its recovery | Reader receives the version subsequently restored | 1 each | 1 each |
| Two fault cases reading after recovery | Reader receives the recovered version | 0 each | 1 each |

All fault runs finish PARTIAL_COMPENSATED with a committed reader and exact scheduled
execution. The three positives prove restoration of startTime, endTime and
numberOfQuestions after the reader received the exact written version. The Topic
course-ID recovery defect remains independently visible: embedded Topic course IDs
become null, leaving one changed Tournament in all eight fault runs. This package does
not fix that application behavior or count the read anomaly as an additional impacted
object.

## Validation and reproducibility

The final executed qualification is `verifiers/target/saga-update-read/source-input-02/`:
14/14 executions pass the independent validator, with six controls and three positive
update-read findings. [runtime-validation.json](runtime-validation.json),
[runtime-provenance.json](runtime-provenance.json) and
[generation-proof.json](generation-proof.json) retain the compact evidence here.

The final mapper-only review safeguard excludes historical CONSTRUCTOR assignments from
post-return mutation checks. It passed its focused regression and regenerated all nine
manifest/package files byte-identically to the executed package. The runtime source
overlay and prepared build are unchanged. This reuses the already executed histories;
it does not claim a second execution of the final-generation directory. The exact hashes
and comparison are in [final-generation-equivalence.json](final-generation-equivalence.json)
and [final-generation-provenance.json](final-generation-provenance.json), with raw output
under `verifiers/target/saga-update-read/source-input-final-generation/`.

Broad regression: **900 tests across 48 suites passed**, including the visitor,
source-backed Quizzes analysis, mapper, model, executor and diagnostic suites. This is
not a full-suite pass: the first broad run had 902 passes and two failures in the existing
ApplicationsFileTreeParserSpec exact Java-file inventory. Its sole unexpected class is
ReadResponseFixture, committed before this package in c7439cb; the parser, inventory test
and Java fixture are unchanged by this work. The passing bounded run excludes that
four-test inventory suite and ScenarioGeneratorApplicationSpec. The latter was stopped
during CPU-bound exact profile-group product counting for Saga sets of size three and
up to 1,000 inputs per Saga. No baseline comparison attributes that performance to this
change. [test-validation.json](test-validation.json) retains suite counts, log hashes,
exclusions and the reproduction command. `git diff --check` passed.

The reproducible runner and its assertions are in
`verifiers/experiments/saga-update-read/source-inputs/`; `source-overlay.json` lists every
current verifier production source compiled over the retained verified runtime. Other
production sources and all retained runtime artifacts are hash checked before and after
execution. Selected package artifacts and the frozen harness are also checked unchanged.

The baseline generation using the earlier extractor stopped at the expected missing
source-setup assertion, after finding the exact two source inputs and six orders. It is
retained under `verifiers/target/saga-update-read/source-input-baseline/`.
The first corrected run, `source-input-01`, executed all 14 cases with exit 0. Its original
validator incorrectly expected prerequisiteSetup=null; the actual report uses the explicit
NOT_REQUIRED record with null provider identity and no bindings. Correcting this assertion
validated the unchanged reports (14 executions, three positives). The final run includes
that harness correction and the two review protections for mutated projections/malformed
fields. No failed run was replaced with a different input or fault selection.

## Changed files and review

Generic implementation: GroovyConstructorInputTraceVisitor, SetupPlanMapper,
SetupValueRecipe, SetupPlanValidator, ScenarioSetupRunner, ExecutableArtifactWriter and
ScenarioCatalogPackageReader. Dummyapp tracing fixture and visitor/mapper/executor Spock
coverage exercise before/after-call isolation, nested exact properties, producer failures,
ordered result mutations, preserved original results and package reading.

Root reviewed the Sol/medium implementation and independently ran the real source-backed
qualification. Review found and closed silent loss of a mutation to a projected property,
silent acceptance of malformed result fields, and a conservative false blocker caused
by confusing original constructor assignments with post-return mutation history. The canonical current-state/roadmap
and this issue package document the result. Inherited diagnostic/type-lookup changes in
the overlay are preceding work, not changes introduced here. No commits, branch changes,
Quizzes edits or meeting-note edits were made by this package.

## Remaining boundaries and next decision

- This qualifies one existing successful source pair, not every retained Update input.
  The separate HashSet/Set constructor representation is still deferred.
- The previous campaign excluded the successful Find input under maxInputsPerSaga and
  used SERIAL/one schedule. This experiment selects that source pair explicitly and uses
  existing interleaving generation; global campaign defaults remain unchanged.
- The retained 665/796 static-candidate count has not been recomputed after this change.
- Read diagnostics remain separate from the persistent-object score. The next research
  decision is how search should use these two observations; this package does not choose
  or implement GA fitness.
- Investigate the expensive exact-accounting test separately, with a baseline comparison;
  the two pre-existing fixture-inventory failures also remain visible rather than being
  absorbed into this input feature.
- Non-read CommitSagaCommand collection gaps still make the control sidecar's overall
  coverage partial, although every selected Find call has a definite negative assessment.
  Fault sidecars have complete coverage within the current supported scope.
