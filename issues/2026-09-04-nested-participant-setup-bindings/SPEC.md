# Nested participant setup-result bindings

Status: implemented and independently reviewed on 2026-09-05; see FINAL-HANDOFF.md.

## What this is

Setup action arguments can already retain returned DTOs nested inside constructors,
assignments and collections. The participant-binding selection in SetupPlanMapper only
recognizes a producer reference at the argument root. Consequently a quiz input containing
`questionDtos = [previouslyCreatedQuestion]` remains blocked, although the preceding exact
question producer is known. The same shape appears in QuestionDto.topicDto collections.

## Goals and evidence

Recover exact earlier setup results inside participant DTO values without erasing mutations,
repeating the measured target, or guessing value identity. Begin with the three CreateQuiz
inputs from StartQuizTest, StartQuizCompensationTest and QuizAnswerEventHandlingTest.
They are a likely +3 over 577, not a proven gain. Inspect the85 CreateQuestion inputs as a
second cohort, not an automatic additional 85: helper occurrence binding, set materialization,
producer order and target exclusion must each hold.

## Non-goals

No cross-test composition, loop/control-flow interpretation, arbitrary property traversal,
event placeholder invention, blind registration, new schema, or broadened impact/search.
CreateTournament's multi-producer helper patterns remain a separately assessed follow-up.

## Functional requirements

- **FR-1:** Detect source-result dependencies recursively in the existing typed recipe,
  including constructor arguments, property assignments, collections and supported transforms.
- **FR-2:** Bind the entire participant argument through the existing SetupValueRecipe
  representation while preserving all literal fields, mutations and collection semantics.
- **FR-3:** Every reference must resolve to an exact retained producer in the same supported
  source context. Missing, ambiguous, cyclic, later or omitted producers remain blocked.
- **FR-4:** Setup must stop before the selected measured target. Restoring a nested binding
  must not turn an incomplete full-fixture candidate into repeated target execution.
- **FR-5:** Keep generation stable and report exact input-level gained/lost candidate IDs.
  Count only plans whose complete argument tuple passes existing validation.
- **FR-6:** Positive/negative dummyapp tests own the generic mechanism; representative
  ordinary Quizzes Docker preflight/replay must prove correct course/question/topic identity.

## Architecture and data

Stay within analysis state → adapter/setup mapper → validator → existing runtime bindings.
Prefer reusing the existing recursive mapValue path and persisted binding shape. An audit
of occurrence preservation and runtime nested materialization precedes source edits.

## Security and operating

Closed application method authorization and exact property policy remain unchanged.
No external effects beyond local source/testing and bounded fresh Docker attempts.
Preserve prior dirty work; update current-state only for actually implemented results.

## Open decisions

No new product policy is needed for the three exact earlier-producer cases. If helper
ambiguity, target replay, missing provenance or a new runtime value language is required,
keep affected cases blocked and split that change rather than loosening the contract.
