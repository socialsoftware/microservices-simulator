# Potential impact beyond deletion: research options

Status: research rationale, not an implemented ImpactV2 contract. The user accepts
reporting potential impact without a developer-supplied business-harm oracle, but rejected
treating references to deleted objects as sufficient breadth. The follow-through
experiment is now under `../impact-updates/`: its RESULTS.md owns current execution
evidence for the Question consumer defect and the no-op compensation mutant. The
source-investigation wording below records the hypotheses before execution; lost-update
reproduction remains future work, not a completed result.

## Thesis intent

`/Users/andre/meic/thesis/André_Silva___IST_UL___MEIC_PIC2.pdf`:

- Section 4.5 proposes deriving execution conclusions without domain-aware generated assertions.
- Section 4.6.1 leaves alternative impact measurements open to empirical comparison using logs, traces and aggregate state.
- Section 5.3 explicitly proposes controlled application mutants, including disabled event handlers and removed compensation steps.
- RQ1 and the evaluation prose still make stronger business-harm claims than a potential-impact indicator can establish. Explain this limitation in the eventual manuscript; a count is an extent signal, not demonstrated severity.

The meeting note `note-04-09-2026.md` also asks for logs/traces, concrete end-to-end cases,
and additional instrumentation if needed, rather than developer-supplied hardcoded rules.

## Concrete update cases

### A failed update leaves its earlier write behind

UpdateTournament writes Tournament properties, then updates its Quiz. It registers an
explicit compensation for the Tournament update using the saved original DTO.
`AbortUpdateAndRetryTest` already asserts the ordinary 2 -> 3 -> 2 question-count recovery.
A controlled variant removing that compensation can leave Tournament count 3 while the
Quiz still contains 2 questions. This variant is proposed, not executed or claimed as an
existing application bug.

A generic observation can identify fields changed by a failed Saga whose effects remain
at the final observation point. For an isolated Saga, the initial values, forward write,
recovery and final values provide a bounded attribution proof. Report one object with
remaining changes, alongside the field differences. Do not count metadata-only version,
timestamp or semantic-lock changes as business-data differences. Intentional residual
side effects remain possible, so the finding is potential impact.

In concurrent runs, before/after inequality alone is insufficient: another successful
operation may legitimately update the same object. Exact write attribution or a stronger
controlled comparison is required; unsupported attribution must remain unknown.

### A copied value remains stale after event processing

AddParticipant retains a UserDto returned by its getUserStep and later passes it to the
Tournament write. UpdateStudentName changes the student's name in CourseExecution and
publishes an event. AddParticipantAndUpdateStudentNameTest asserts a temporary old name
in Tournament and its repair after event handling. The creator variant also asserts a
local invariant rejection followed by successful retry, not lasting data damage.

A controlled disabled/no-op name-update handler is a useful proposed faulty comparison.
Observe the exact source/receiver identity, relevant data value, subscription/event,
handler attempts and final receiver state. Count the receiver once if a supported
source-to-copy relationship remains stale at the declared observation point. An ordinary
old snapshot or a pending unattempted event is not proof of a failed repair.

Calling the event handler once before a non-creator joins does not establish a permanently
lost event. Ordinary EventApplicationService re-queries retained events against current
subscriptions and versions; later delivery may repair it. A suitable observation horizon
must include relevant processing/retries or explicitly report that the run did not settle.

### A real source-level update-propagation defect candidate

`QuestionService.updateQuestion` persists the new Question and emits UpdateQuestionEvent
(lines 75-83). Active Quizzes subscribe through their embedded QuizQuestion identity and
version (`Quiz.getEventSubscriptions`, `QuizSubscribesUpdateQuestion`). The consumer
`QuizService.updateQuestion` (lines 177-189) creates a fresh Quiz, changes its embedded
question title/content/version, and returns without `registerChanged`. The Quiz copy
constructor deep-copies QuizQuestion instances (lines 82-95), so this is not an update to
the managed old embedded object.

This is a concrete source-supported defect candidate, not a newly reproduced runtime
result. Expected evidence is a persisted publisher update, eligible delivery to the
exact Quiz, successful consumer return, and no persisted receiver update. Ordinary
EventRepository queries retained published events whose publisher version exceeds the
subscriber's version; failure to persist that progress can make the same event eligible
again on later polls.

A proposed observation is a still-stale linked value after relevant event-processing
attempts, with repeated identical event/receiver delivery and no persistence progress as
supporting evidence. Neither zero writes alone nor version inequality alone proves a
problem: idempotent/irrelevant events and intentionally historical snapshots are controls.
Use the existing subscription and observed value relationship rather than guessing
relationships from equal field names. This candidate should be reproduced before adding
a production detector or repairing the application.

### Compensation overwrites another successful change

An illustrative lost-update pattern is: A saves an old object; B successfully changes a
field; A fails and restores the whole saved object, erasing B's change. This is a general
candidate, not a reproduced Quizzes defect. Quizzes UpdateTournament restores selected
properties through a service that copies current state; its semantic locks also block
several conflicting operations. AnonymizeStudentAndUpdateTournamentTest exercises
compensation followed by successful event retry, preserving anonymization. It is a useful
negative control against counting every final difference as a failed rollback.

Detection needs exact read/write versions and field changes, plus evidence that the
stale restoration erased the intervening write. Two writes to the same object alone do
not prove a lost update. Full dependency-graph analysis and serial comparison are broader
options; neither is required for the first bounded update experiment.

## Proposed sequence and reporting

1. Retain the proven deleted-target condition as one finding category.
2. Compare ordinary UpdateTournament compensation with a controlled missing-compensation
   variant, observing residual data changes. Use an isolated Saga first.
3. Reproduce the Question-to-Quiz propagation defect candidate, then compare with working
   propagation. Use the student-name tests as additional repaired/guarded controls and
   an explicit event-processing boundary with exact source/receiver observations.
4. Extend to surviving copies of compensated-away update values, or lost-update histories,
   only when exact provenance can be established. Read exposure alone remains diagnostic.

Start with a matrix of distinct affected objects by category. If a scalar is needed,
a simple proposed total is the union of objects with supported persistent-effect findings,
so multiple symptoms on one object do not inflate it. Keep categories and observation
coverage beside the number. This total and its categories require agreement and validation;
they have not replaced the production invariant-event ImpactV1 score.

## Instrumentation boundary

The current generic AGGREGATE_ACCESSED event records access mode, aggregate type/ID,
source method and execution context. It does not provide the exact returned aggregate
version or field-level before/after data. The context's unit-of-work version is not a
substitute for those facts. Existing persisted versions and command/event hooks provide
places to add bounded evidence, but tracing a read and a later write in the same Saga
does not by itself prove that a particular value was copied.

Source evidence was inspected, not newly executed for this investigation. The earlier
six behavior probes and original deletion experiment retain their separate run evidence.

Classic dirty-read, lost-update and read/write-skew patterns are useful vocabulary for
additional observations; see Berenson et al., [A Critique of ANSI SQL Isolation Levels](https://www.microsoft.com/en-us/research/publication/a-critique-of-ansi-sql-isolation-levels/).
Their transaction scope must not silently be equated with a whole Saga, whose local steps
can commit independently. Detecting a read/write pattern alone also does not establish a
surviving final-state effect.
