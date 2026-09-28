# RQ1 subject inspection and pilot candidates

The direct-switch and trailing-default-helper gaps below were addressed on 20 September.
Current uncapped counts and remaining event/setup limitations are in
[the three-variant recount](../evidence/three-variants-2026-09-20/RESULTS.md).
The measurements below remain the historical pilot.

**Decision after the 18 September meeting:** include all three variants in the paper,
explaining differences and comparing counts. This supersedes the internal-probes-only
recommendation below. The extraction diagnoses remain relevant; comparable counts need
those gaps assessed and resolved where they affect the result. See the
[paper revision plan](paper-revision-plan-2026-09-18.md).

This records source inspection and a bounded static extraction pass over the three
current checkouts. No application, verifier or active campaign code was changed.
The follow-on scheduling/compression experiment is defined in
[`generation-comparison/README.md`](../../../verifiers/experiments/generation-comparison/README.md).
Its first run is under `verifiers/target/generation-comparison-2026-09-18-run1/`;
it completed successfully. Durable [results and graphs](../evidence/generation-comparison-2026-09-18/RESULTS.md)
cover all 74,481 Saga sets at sizes 2–4 for each input cap, with 20 complete-enumeration
checks and 3,996 accounting comparisons. Under these input bounds, strict selection
retained pairs but no triples or quadruples; larger-set compression results therefore
use the explicitly broader conflict lens.

## Bounded static pass

Raw evidence: `verifiers/target/rq1-static-pilot-2026-09-18/`, including commands,
logs, source hashes, a build proof and `summary.json`. All three runs completed.
Current verifier Java sources were compiled into an isolated directory with JDK 21;
dependencies came from the retained executable JAR. An earlier JAR-only probe was
stopped and retained under `discarded-jar-probe-quizzes`; it contributes no results.
No frozen campaign dependency was rebuilt or overwritten.

Common configuration: COUNT_ONLY, INTERACTION_PRUNED, strict input evidence,
SEGMENT_COMPRESSED, Saga sets of size 1–2, three inputs per Saga, schedule cap 5,000,
dynamic enrichment disabled. The event cap remains one (the configuration requires
at least one); this static pass writes no workload or fault-scenario schedules.
Runs were sequential, with a 1.5 GiB Java heap and two visible processors, alongside
the runtime campaign. Elapsed times are operational observations, not performance results.

| Measurement | quizzes | quizzes-full | quizzes-full-2 |
| --- | ---: | ---: | ---: |
| Extracted Sagas | 68 | 43 | 46 |
| Inputs retained under the three-input cap | 89 | 43 | 42 |
| Single-Saga static setup candidates, including source setup | 72 | 12 | 0 |
| Strict Saga pairs with accepted compatible inputs | 9 | 0 | 0 |
| Broader type-only Saga pairs with accepted inputs | 248 | 4 | 109 |
| Resolved event routes | 16 | 0 | 0 |

These are different counts: a Saga pair is not a workload count, and static setup
candidates are not successful executions. Broad pairs are accounting evidence under
the alternative policy, not extra rows written by this strict run.

To test whether the input cap explains the missing strict pairs, both full variants
were repeated with cap 1,000, above their extracted input counts. Full retained 292
inputs and had 167 single-Saga setup candidates; full-2 retained 107 inputs and had
zero. Both still had zero strict pairs with accepted inputs. The missing pairs are
therefore not resolved merely by increasing this cap.

Concrete findings:

- Full's TournamentCommandHandler calls services directly inside the public switch.
  CommandHandlerVisitor.mapCommandsToDispatchInfo currently indexes private methods
  with typed Command parameters. Consequently AddParticipant's four extracted steps
  have no command accesses and explicit UNRESOLVED_COMMAND_DISPATCH diagnostics.
  The small interaction count must not be interpreted as a property of the application.
- Full-2's private handler methods expose those accesses successfully, but its
  AddParticipant input retains createTournament/createActiveUser as unsupported helper
  results. No static setup candidate was recovered in this pass. A full helper/input
  diagnosis is separate work; absence of candidates is not absence of application tests.
- The proposed UpdateQuiz pair has no source input in the original quizzes snapshot,
  while full-2's GetTournamentById has none. These are provisional candidate families,
  not a qualified common benchmark yet.

Recommendation: retain all three as internal extraction probes, use quizzes for the
first compression pilot, and only add a full variant to the paper comparison after its
extraction is sufficient for the intended claim. Do not publish extraction gaps as a
compression win or change to type-only silently. Whether to support the direct-switch
handler shape is a bounded follow-up decision, not part of these runs.

## Remote-branch check

After fetching origin, the named `origin/quizzes-full` and `origin/quizzes-full-2`
application trees match this checkout. Other branches do contain changes:

- `origin/master` changes Full-2 event handling and rejects non-advancing publisher
  versions (`334fe8550`), and normalizes persisted temporal test fixtures
  (`17784c7ab`, `7f94539f7`). Full differs there only in a documentation link.
- `origin/feature/consistency-testing-random-baseline` adds Full event-handling
  interfaces and consistency-sweep catalogues/tests.

The inspected deltas do not change Full's direct-switch command dispatch or replace
Full-2's domain setup helpers with supported input recipes. These branches were not
merged, built or reanalyzed. Continue the generation comparison with Quizzes; the
Full variants remain internal probes rather than additional paper subjects by default.

## Subjects

| Source inventory | quizzes | quizzes-full | quizzes-full-2 |
| --- | ---: | ---: | ---: |
| Services declared by ServiceMapping | 8 | 8 | 8 |
| Java files under src/main | 440 | 268 | 288 |
| Saga classes under coordination/sagas | 68 | 43 | 46 |
| Groovy files under src/test, including support code | 64 | 95 | 90 |
| Groovy files under coordination test directories | 37 | 70 | 66 |
| Declared steps in CreateTournament | 7 | 8 | 6 |
| Declared steps in UpdateTournament | 5 | 4 | 6 |
| Declared steps in AddParticipant | 2 | 4 | 4 |

Counts are filesystem/source inventory, not accepted inputs, passing tests, executable
workloads or generator-discovered totals. Step counts count literal SagaStep declarations;
multiple commands inside one loop do not become additional declared steps.

These are related implementations of the same domain, not a small/medium/large series.
The two full variants have application-generation plans in their respective roots.
They should not be described as three independent application domains or as strict
supersets of quizzes.

## Differences that affect the evaluation

- **Tournament updates:** quizzes reads the old Tournament and topics, updates the
  Tournament, selects questions and updates the Quiz. Full has four steps and sends
  updated dates without reselecting the question list. Full-2 has six steps: topics,
  Tournament, Quiz, question selection, Tournament update, Quiz update. Its Quiz read
  preserves the existing title when constructing the subsequent update.
- **Participant addition:** quizzes has two steps; full and full-2 have four.
  Full reads User, Student, Tournament and then adds the participant; full-2 reads
  User, Tournament, Execution and then adds the participant. This changes both the
  possible orders and the shared aggregate accesses.
- **Starting an answer:** quizzes has StartQuiz (three steps), full has
  CreateQuizAnswer (three), and full-2 has CreateQuizAnswer (five, additionally reading
  Execution and Questions). Full wraps its Quiz/User reads with READ_QUIZ/READ_USER
  semantic locks; full-2's corresponding prerequisite reads are plain commands.
  Names containing “read” therefore do not establish identical effects.
- **Test inputs:** full helpers commonly return DTOs; full-2 commonly builds DTOs with
  setters and returns their aggregate IDs. Its createTournament helper also creates
  questions inside a Groovy times closure. The pilot must report what the current
  extractor accepts and prepares; source test counts do not establish that coverage.
- **Events:** inspected full/full-2 event-processing paths delegate to facade methods
  which call services and commit directly, without constructing a Saga. For example,
  full-2 ExecutionFunctionalities.setStudentNameByEvent uses this shape. Current event
  consequence generation requires a resolved consumer Saga. Do not assume equivalent
  event-route coverage across variants; audit diagnostics separately.
- All three declare Java 21, simulator 3.2.0-SNAPSHOT and Saga/local test profiles.
  This inspection did not build them or establish current runtime compatibility.

## Proposed first pilot

Begin with normal Saga actions and no selected event consequences, explicitly scoped
as scheduling evaluation. Keep source-derived inputs and report unsupported recipes.
Do not modify tests or invent inputs just to obtain a favourable compression ratio.

Use these operation families, subject to the generator actually accepting their inputs:

| Family | Question |
| --- | --- |
| UpdateTournament + FindTournament/GetTournamentById | What orders remain between a multi-step update and a one-step query? |
| UpdateTournament + AddParticipant | What reduction remains when two workflows access the Tournament, with other prerequisite accesses? |
| UpdateTournament + AddParticipant + FindTournament/GetTournamentById | How does the same story scale from two to three Sagas? |
| UpdateQuiz + StartQuiz/CreateQuizAnswer | Does the different read/lock structure yield a different set of conflict anchors? |

For the participant/update pair, preserving each Saga's listed order gives theoretical
uncompressed step interleaving counts of 21, 70 and 210 respectively. Adding the one-step
Tournament query gives 168, 630 and 2,310. These are multinomial calculations from the
declared lengths, per input tuple, before extra constraints. They are not measured
WorkloadPlan/FaultScenario counts and include neither event nor recovery alternatives.
They suggest that full materialisation is affordable for an initial narrow comparison.

Suggested first input bound: one accepted variant per selected Saga, chosen by a fixed
documented rule; then three to assess input sensitivity. Record exact input IDs and
whether strict evidence establishes the intended common object. A missing strict match
must remain visible, not be silently replaced with a type-only result.

Separate the comparisons:

1. Fix Saga/input tuples and conflict evidence; compare ORDER_PRESERVING_INTERLEAVING
   with SEGMENT_COMPRESSED. Measure full/capped counts, retained conflict-anchor orders,
   materialisation time and peak memory.
2. Fix scheduling and input bounds; compare BRUTE_FORCE with INTERACTION_PRUNED under
   strict and type-only evidence. Report selected sets and input tuples, not only the
   number of output files. A broader whole-application count-only pass follows the pilot.

Count-only timing measures counting, not the cost avoided by actually generating all
orders. Timing runs used in the paper should be isolated from the active runtime campaign.
Compression preserves orders under the extracted conflict model; this experiment alone
does not establish preservation of every harmful runtime execution.

## Source pointers

The application paths below are relative to the repository root:

- applications/{quizzes,quizzes-full,quizzes-full-2}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{quizzes,quizzesfull,quizzesfull2}/ServiceMapping.java
- The same package roots: microservices/tournament/coordination/sagas/{CreateTournament,UpdateTournament,AddParticipant}FunctionalitySagas.java
- Full variants: microservices/quizanswer/coordination/sagas/CreateQuizAnswerFunctionalitySagas.java
- Full-2: microservices/execution/coordination/functionalities/ExecutionFunctionalities.java
- Full variants' src/test/groovy package roots: QuizzesFullSpockTest.groovy and QuizzesFull2SpockTest.groovy; sagas/coordination/tournament/UpdateTournamentTest.groovy
- [Compression contract](../decisions/2026-06-16-conflict-anchor-segment-compression.md).

The static target is configurable through VERIFIERS_APPLICATION_BASE_DIR; no dedicated
application-specific generation command is required. Ordinary generation is still needed
to establish extraction coverage and exact workload IDs for these candidates.
