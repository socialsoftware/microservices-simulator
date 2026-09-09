---
name: fix-consistency-defect
description: Reproduce reviewed consistency-sweep defects with deterministic regression tests, then repair and verify them. Use only after a review identifies concrete application defects; do not use for report interpretation alone.
---

# Fix Consistency Defect

Turn one reviewed, actionable consistency finding, or a confirmed set of such findings, into tested application repairs. This skill is separate from `review-consistency-sweep`: review remains read-only.

## 1. Establish evidence and scope

Require an explicit target application and decisive review report path(s). For each finding, require its report trace ID when the report assigns one; otherwise assign a stable local identifier in the issue ledger. Read governing `AGENTS.md` files, the named report record and decisive run report(s), the coverage manifest/provider, relevant production code, and existing tests.

Before editing, recheck only issue-specific evidence: report conclusions can be stale, inferred causes can be wrong, and application code may have changed since review. This is not a second full campaign review.

For multiple findings, create an issue ledger before editing. Record each finding's identifier, classification, confidence, decisive evidence, harmful postcondition, reproduction test, affected production regions, proposed repair group, and status. Show the user the ledger and proposed repair groups before editing, then ask whether the plan is acceptable. Keep the ledger current as each finding moves through reproduction, repair, verification, or blockage; continue through the full confirmed set rather than losing context after one issue.

Group findings by demonstrated root cause and inseparable production change, not by file proximity. Use separate repair groups for independent causes. Batch findings only when one change and its tests address them together; otherwise keep them separate. Each ready repair group gets its own suggested commit message, but do not stage or create commits. Keep all application changes unstaged for user review.

Classify the finding before editing. Continue only for `probable-consistency-defect` with a concrete harmful final-state claim. For `expected-business-rejection`, `catalog-or-invariant-defect`, `engine-limitation-or-instrumentation`, or `inconclusive`, report why a product repair is not justified and stop unless the user changes scope.

State the exact postcondition the regression test will prove. Do not turn a generic isolation anomaly into an application bug without code and domain evidence.

## 2. Reproduce before fixing

Write one deterministic application-level regression test per repair group that follows the reported schedule as closely as public functionality/workflow test APIs permit. It must:

- construct only valid initial state;
- preserve the reported ordering of reads, writes, aborts, compensation, and commits;
- assert the claimed harmful committed state, not an implementation detail or oracle status;
- use a fresh persistence read where final committed state matters.

While changes remain unstaged for review, optionally mark each regression test and changed production region with one temporary nearby comment:

```java
// CONSISTENCY-SWEEP-DEFECT: <finding-id> — <what changed and why>; evidence: <review report path and decisive report/run identifier>
```

Use the review's trace ID when one exists. Otherwise use a stable local identifier for the issue ledger. These marks are optional review aids, not a correctness requirement; keep or remove them according to the user's preference.

Run that test alone before modifying production code. A passing test, compilation failure, or a failure unrelated to the claimed postcondition is not a red reproduction.

If exact reproduction is impossible, do not guess a repair. Explain whether missing schedule detail, absent seed, oracle/runtime semantic differences, nondeterministic isolation, or an invalid report hypothesis blocked it. Preserve the test only when it is a useful faithful partial reproduction; otherwise leave no speculative test or code change.

## 3. Repair minimally

After a red test proves the problem, make the smallest production change that restores the stated postcondition across the operation boundary. Prefer correct ownership, lifecycle, locking, or compensation semantics over timing, sleeps, test-only branches, swallowed errors, catalog exclusions, or weakened assertions.

Do not change `simulator/` during this workflow. It is a `2-fw` decision under the repository harness rules and needs explicit human direction.

Keep application changes unstaged. Do not change catalog coverage or inter-invariants merely to make the report disappear. Add an executable invariant only when it is independently correct and safely observable.

## 4. Verify and iterate

Run the new regression test after each repair. It must pass for the asserted final-state property. If it still fails, inspect cause and make another minimal repair; do not weaken the test.

Then run the smallest relevant existing test class or suite. Update the issue ledger after each result and send concise progress updates at reproduction, repair, verification, and blockage milestones. Run the application test profile's clean full suite before handoff when time and scope permit. Report command exit status and surefire results; a test pass without a successful build is not verified.

## 5. Hand off

Report:

- reviewed finding and claimed invariant;
- red-test result, or precise reproduction blocker;
- production change and why it addresses root cause;
- regression and relevant-suite results;
- issue ledger, repair-group boundaries, and a suggested commit message for each ready group;
- unstaged application paths.

Remind the reviewer that temporary `CONSISTENCY-SWEEP-DEFECT` annotations may be removed before staging or committing, and ask for their preference before any such cleanup. If a finding is blocked, preserve its ledger entry and state the exact missing evidence or decision instead of silently skipping it.

Do not rewrite the review report unless the user asks. If this workflow revealed reusable harness guidance, follow `AGENTS.md` harness-evolution gates before changing skills or documentation.
