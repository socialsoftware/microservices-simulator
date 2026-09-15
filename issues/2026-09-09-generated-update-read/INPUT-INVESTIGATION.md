# Why the retained update/read inputs lack a complete setup

Read-only investigation requested after the generated qualification. Luna/xhigh audits
existing Quizzes tests; root independently audits the extractor and runs static
counterfactuals. No production fix is included here.

## Correction to the earlier explanation

The thirteen retained input facts have `materializable=false`, but that standalone flag
does not mean none can be supplied by source setup. Fresh extraction finds one setup
candidate for each of the seven FindTournament inputs. It finds none for the six
UpdateTournament inputs. Consequently none of the nine pairs has a complete shared setup.
The Tournament creation result itself is recognized. The main blocker is more specific
than an inability to understand the test helpers.

## Confirmed blockers and consequences

1. **Properties of setup results inside collections are deliberately excluded.**
   Tests build `[topicDto1.aggregateId, topicDto2.aggregateId, ...].toSet()`.
   `SetupPlanMapper.invalidParticipantReference` rejects nonempty property paths on
   nested references with `UNSUPPORTED_NESTED_SETUP_RESULT_PROPERTY:[aggregateId]`.
   All six retained Update inputs have this exact full-fixture blocker. The earlier
   nested-binding change intentionally covered whole returned DTO objects, not their
   scalar properties inside recipes; see the September 4 nested-binding handoff.

2. **Returned DTO mutations need preservation before those inputs can be trusted.**
   `UpdateTournamentTest.setup()` creates a Tournament with two questions (line 83).
   Its successful feature changes dates and sets numberOfQuestions to three (lines
   100–103), then passes the returned DTO to updateTournament (line 107).
   `applyScopedMutations` only attaches mutations to CONSTRUCTOR recipes; this DTO
   comes from a runtime facade result through a helper. The mapper binds it directly
   to the earlier ACTION_RESULT, with no assignments. It does not preserve the requested
   update. Existing support for setters on freshly constructed DTOs does not cover this
   returned-object case.

   In a disposable copied mapper, permitting only nested `aggregateId` references
   raises source-setup candidacy from zero to five of the six retained Update inputs.
   For the success input `23becf1d...`, argument 1 is simply setup-action-14's result,
   assignments=[], while argument 2 correctly becomes a set of three exact earlier
   action-result IDs. The creation DTO still specifies two questions. This demonstrates
   why simply deleting the guard would qualify the wrong update. This is static evidence,
   not a successful runtime qualification of the hypothetical fix.

3. **One additional collection-constructor shape remains blocked.**
   RemoveTournamentAndUpdateTournamentTest uses `new HashSet(Arrays.asList(...))`.
   The recipe retains targetTypeText=HashSet but targetTypeFqn=Set<Integer>.
   SetupPlanMapper's constructor handling recognizes java.util.HashSet, so the
   counterfactual still rejects this input as UNSUPPORTED_SETUP_CONSTRUCTOR. Runtime
   erasure of provider-bound Set<Integer> types, fixed in the preceding task, does not
   repair this separate static constructor representation.

4. **The retained catalogue's selection also matters.**
   Eight of nine retained pairs combine inputs from different feature methods in the
   same test class; that alone is not forbidden, since complete common setup is supported.
   Some features deliberately use invalid updates or read after removal/recovery.
   They cannot all be assumed to be successful baseline histories. The existing
   FindTournament call in `UpdateTournamentTest#update tournament successfully`, input
   `e754c1e46e...`, is present in inputs.jsonl but rejected with
   notAcceptedReason=maxInputsPerSaga. The campaign's 10-input bound retained earlier
   stable IDs instead. The clean test already exists; it did not require writing a new
   Quizzes story to discover it.

## Smallest recommended next implementation

Start with the existing successful UpdateTournament test. Preserve exact earlier-result
identity and the ordered pre-call assignments on its returned DTO, then permit the
already-supported aggregateId property projection inside a collection. Preserve the
original setup result when another participant needs it; alias handling and the exact
point at which mutations apply must be explicit. Do not introduce arbitrary getters,
whole-test execution or inferred IDs to obtain a positive count.

Add dummyapp positive/negative cases for returned-result mutation, collection properties,
wrong/missing producer occurrences, and later assignments not leaking into earlier inputs.
Qualify the existing successful test's exact pair with an explicit target selection so
its useful read is not dropped by the broad campaign's input prefix. Check concrete
materialized dates/question counts, not merely a higher static candidacy count.

The HashSet constructor shape can follow independently. Negative tests and reads after
manual workflow/assertion barriers remain visible without being misrepresented as clean
controls. Do not conflate input reuse with replaying their entire original test history.

## Evidence

- [Compact input-by-input audit and counterfactual bindings](input-investigation.json).
- Raw current extraction: `verifiers/target/saga-update-read/input-audit-01/`.
- Disposable guard-only counterfactual: `verifiers/target/saga-update-read/input-audit-counterfactual/`.
- Existing boundary: `issues/2026-09-04-nested-participant-setup-bindings/HANDOFF.md`.
- Code anchors: SetupPlanMapper.java (participant mapping and invalidParticipantReference),
  GroovyConstructorInputTraceVisitor.java (applyScopedMutations), ScenarioSetupRunner.java
  (ACTION_RESULT evaluation), and the Quizzes tests cited above.

Both extraction runs use the retained verified unchanged source-analysis/application
build from empty-event-delivery/run-01. The counterfactual shadows only a copied mapper;
no target scenario was executed and no production source was edited. Audit artifacts
retain exact commands, source copies and hashes.

Luna/xhigh independently confirmed the test/helper return-and-rebinding pattern, the
post-return mutations in UpdateTournamentTest and AddParticipantAndUpdateTournamentTest,
and the separate HashSet shape in RemoveTournamentAndUpdateTournamentTest. It reported
no disagreement with the root audit. Its work was source inspection, not runtime tests.
