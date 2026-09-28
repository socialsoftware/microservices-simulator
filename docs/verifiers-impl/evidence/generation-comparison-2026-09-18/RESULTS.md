# Quizzes: static generation comparison

Exact counts over all combinations of 2–4 distinct Saga types with accepted inputs, under each declared input cap. Forward schedules only: no event expansion, prerequisites, faults or recovery schedules. Accepted input recipes may still be unsupported by the executor. These are not counts of runnable FaultScenarios.

## Input selection

| Input cap | Sagas per set | All sets | Strict sets | Broad sets | All tuples | Strict tuples | Broad tuples |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 2 | 666 | 3 | 248 | 666 | 3 | 248 |
| 1 | 3 | 7770 | 0 | 2216 | 7770 | 0 | 2216 |
| 1 | 4 | 66045 | 0 | 17612 | 66045 | 0 | 17612 |
| 3 | 2 | 666 | 9 | 248 | 3841 | 14 | 1481 |
| 3 | 3 | 7770 | 0 | 2216 | 107085 | 0 | 31291 |
| 3 | 4 | 66045 | 0 | 17612 | 2167713 | 0 | 595123 |

Broad means strict evidence plus type-only fallback, not type-only matches alone.

## Compression on identical selected inputs

| Input cap | Sagas per set | Conflict lens | Full orders | Compressed orders | Reduction |
| --- | --- | --- | ---: | ---: | ---: |
| 1 | 2 | strict | 10 | 7 | 30.00% |
| 1 | 2 | broad | 26248 | 2143 | 91.84% |
| 1 | 3 | strict | 0 | 0 | not applicable |
| 1 | 3 | broad | 1159010247 | 15530960 | 98.66% |
| 1 | 4 | strict | 0 | 0 | not applicable |
| 1 | 4 | broad | 201267820967830 | 1678192829898 | 99.17% |
| 3 | 2 | strict | 74 | 46 | 37.84% |
| 3 | 2 | broad | 86117 | 8961 | 89.59% |
| 3 | 3 | strict | 0 | 0 | not applicable |
| 3 | 3 | broad | 4626288619 | 87153627 | 98.12% |
| 3 | 4 | strict | 0 | 0 | not applicable |
| 3 | 4 | broad | 1083376568484336 | 13162997272310 | 98.79% |

Totals weight every selected input tuple equally. The same sequence of Saga steps with different inputs counts separately. A reduction in the static space does not establish preserved runtime outcomes or a measured execution speedup.

## Enumeration checks

20 deterministically selected cases passed complete enumeration, exact count agreement, uniqueness, in-Saga step order and equality of whole conflict-anchor order sets. The independent totals also matched 3996 ordinary accounting rows/configurations after applying its schedule cap.

| Sagas | Lens | Full orders | Compressed orders |
| --- | --- | ---: | ---: |
| RemoveCourseExecution + UpdateStudentName + AddParticipantAsync + LeaveTournament | type-fallback | 1680 | 630 |
| GetCourseExecutionById + GetCourseExecutions + RemoveStudentFromCourseExecution + UpdateStudentName | type-fallback | 60 | 60 |
| RemoveCourseExecution + UpdateStudentName + FindQuiz + CreateTournamentAsync | type-fallback | 9240 | 420 |
| UpdateUserNameInQuizAnswer + FindQuiz + AnonymizeUserTournament + SolveQuizAsync | type-fallback | 720 | 210 |
| CancelTournament + FindTournament + RemoveTournament | type-fallback | 60 | 30 |
| GetCourseExecutions + RemoveCourseExecution + SolveQuizAsync | type-fallback | 1320 | 12 |
| AddParticipant + AnonymizeUserTournament + FindTournament + UpdateUserName | type-fallback | 420 | 60 |
| AddParticipant + CreateTournament | type-fallback | 36 | 6 |
| UpdateStudentName + CreateTournament + UpdateTournament | type-fallback | 10296 | 2310 |
| FindTournament + LeaveTournament + RemoveTournament | type-fallback | 60 | 30 |
| AnonymizeStudent + CreateCourseExecution + RemoveStudentFromCourseExecution | type-fallback | 420 | 30 |
| RemoveQuizAnswer + AddStudent + SolveQuizAsync | type-fallback | 360 | 12 |
| AddStudent + GetCourseExecutionsByUser + UpdateStudentName + CreateUser | type-fallback | 60 | 60 |
| GetCourseExecutionsByUser + CreateTournament | type-fallback | 8 | 2 |
| CreateTournamentAsync + SolveQuiz | type-fallback | 1716 | 56 |
| StartQuiz + SolveQuiz | type-fallback | 120 | 10 |
| CreateQuestion + SolveQuiz | type-fallback | 330 | 2 |
| CreateTournamentAsync + CreateTournament | type-fallback | 1716 | 462 |
| RemoveStudentFromCourseExecution + UpdateStudentName | strict | 3 | 3 |
| DeactivateUser + DeleteUser | strict | 3 | 3 |

## Scope and interpretation

- Source hashes, exact input IDs and all Saga-set rows accompany the results.
- No schedule-count cap is applied to the totals shown here; no sampling of Saga sets at sizes 2–4.
- Input caps 1 and 3 are explicit bounded input populations, not all original tests or possible application inputs.
- Preservation is checked against the extracted conflict model, not a universal application-correctness oracle.
- The enumerated subset is selected by Saga count, full-order size and conflict lens, with SHA-256 ordering; compression ratio is not a selection criterion.
- Runtime, memory, GA quality and impact are not measured by this experiment.
