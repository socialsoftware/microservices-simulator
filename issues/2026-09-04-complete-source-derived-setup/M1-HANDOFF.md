# M1 — Bounded nested result path

State: complete. Covers FR-14–18 and the nested-path part of FR-19–20.

`quiz.aggregateId` is represented in the existing result-property string. Mapping admits
that exact two-segment path; validation keeps the existing DTO-root authority. Before
any dispatch, runtime checks the public getter chain, its DTO intermediate, and its
Integer-compatible leaf even when surrounding metadata provides no expected type.
Null intermediate results fail setup. Existing one-property behavior is unchanged.

Actual implementation files:

- `SetupPlanMapper.java`, `SetupPlanValidator.java`, and `ScenarioSetupRunner.java`;
- `ScenarioExecutorSpec.groovy` and new `SetupPlanMapperNestedPropertySpec.groovy`;
- dummyapp `ItemDto.java` and one nested-reference method in `GroovySagaTracingSpec.groovy`;
- affected current-state, roadmap, ordered-setup ADR, and artifact audit sections.

The coordinator implemented this slice; Luna/max independently reviewed it and passed
the corrected final diff. Review identified that untyped collection elements could omit
expected/declared type metadata; unconditional Integer leaf checking and two no-dispatch
signature regressions close that case. No new Quizzes FQN, arbitrary property traversal,
record field, recipe kind, dispatcher method, or runtime input identity was added.

Proof:

- Focused executor/parent/mapper run: 136 + 15 + 1 tests, no failures/errors/skips.
- After signature strengthening, `ScenarioExecutorSpec`: 139 tests, no failures/errors/skips.
- `SourceDerivedSharedSagaWorkloadAnalysisSpec`: pass.
- Positive package round-trip resolves a distinct child id (11007 rather than parent
  1007); negative checks cover null intermediate, missing getter, wrong DTO/leaf types,
  raw collection metadata, wrong root, and unapproved paths. Package hashes are stable.
- Fresh ordinary full-single generation: 796 accepted inputs, 562 with source setup,
  one needing no setup, 233 blocked (563 static candidates, up from 560).
- All three newly covered FindQuiz workloads passed selected isolated Docker preflight
  in 49.54 seconds, each resolving `quiz.aggregateId` and starting its exact participant.

Evidence: `verifiers/target/astra-m1-singles/quizzes-20260904-213113-085/` and
`verifiers/target/astra-qualification/nested-path-{ids.txt,preflight.json,preflight.log}`.
The later unconditional leaf check is additionally proven by the focused runtime tests;
the Docker positive inputs all have the required Integer leaf.

The three inputs originate from inspection calls in UpdateTournamentTest,
RemoveTournamentAndUpdateTournamentTest, and RemoveTournamentTest. This is a bounded
value-reconstruction/startup result under fixture-only setup, not original whole-test
replay or a new harmful benchmark. The remaining 175 partial and 58 missing binding
cases belong to the independent feature-prefix slice.
