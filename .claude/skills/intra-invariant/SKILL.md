---
name: intra-invariant
description: Add a Layer 1 intra-invariant (verifyInvariants check) to an existing aggregate. Arguments: "<AggregateName> <rule-description>"
argument-hint: "<AggregateName> <rule-description>"
---

# Add Intra-Invariant: $ARGUMENTS

You are adding a new **Layer 1 intra-invariant** to an existing aggregate in `applications/quizzes`.

An intra-invariant is a consistency rule that must always hold within a single aggregate instance. It is checked by `verifyInvariants()` on every Unit of Work commit, regardless of which operation caused the change.

> If this rule depends on state from another aggregate, use Layer 4 (`/inter-invariant`) instead. Do NOT throw domain exceptions from mutation methods — all aggregate state rules belong in `verifyInvariants()`. Consult `docs/concepts/consistency-enforcement.md` if uncertain.

---

## Step 0 — Parse the arguments

From `$ARGUMENTS` identify:
- **AggregateName**: the aggregate class to modify (e.g. `Tournament`, `Execution`, `Quiz`)
- **Rule description**: what the invariant must enforce
- **State scope**: does the rule apply to ALL states, ACTIVE only, DELETED only, or a custom combination? (Document this decision before proceeding.)

---

## Step 1 — Read existing code

Before writing anything, read:
1. The aggregate class under `microservices/<aggregate>/aggregate/<Aggregate>.java` — focus on:
   - The class-level `INTRA-INVARIANTS` comment block
   - Existing private `boolean invariantXxx()` helpers
   - The `verifyInvariants()` method (state-scoping blocks)
2. `QuizzesErrorMessage.java` — existing error constants
3. The closest test class under `src/test/groovy/.../sagas/coordination/<aggregate>/`

---

## Step 2 — Design the invariant helper

Decide:
- **Method name**: `invariant<RuleName>()` (e.g., `invariantUniqueParticipant()`, `invariantAnswerBeforeStart()`)
- **Return type**: `boolean` — return `true` if the invariant holds, `false` if it is violated
- **State scope strategy**: should the check run for all states, or only when `getState() == AggregateState.ACTIVE`?
  - Rules involving business-logic fields typically apply to ACTIVE aggregates only
  - Rules involving structural integrity (e.g., referential consistency) may apply to all states
- **Error constant**: reuse `INVARIANT_BREAK` for generic violations, or add a rule-specific constant (see Step 5)

Document the decision in a brief comment before writing code.

---

## Step 3 — Add the private boolean helper

Add a private method to the aggregate class with a `/* RULE_NAME */` doc comment:

```java
/*
 * RULE_NAME
 * <formal statement of the rule, e.g.:
 *   this.participants.size() <= this.maxParticipants>
 */
private boolean invariantRuleName() {
    // return true if the invariant holds
    return <condition>;
}
```

---

## Step 4 — Call from `verifyInvariants()`

Add the call inside `verifyInvariants()` in the correct state-scoping block.

The typical structure mirrors what already exists — for example, in `Tournament.java`:
```java
@Override
public void verifyInvariants() {
    // DELETE invariant — applies to all states
    if (!invariantDeleteWhenNoParticipants()) {
        throw new QuizzesException(INVARIANT_BREAK, getAggregateId());
    }
    // ACTIVE-only invariants
    if (getState() == AggregateState.ACTIVE) {
        if (!(invariantAnswerBeforeStart()
                && invariantUniqueParticipant()
                /* ... */
                && invariantRuleName())) {  // ← add here
            throw new QuizzesException(INVARIANT_BREAK, getAggregateId());
        }
    }
}
```

If the rule applies to all states, add it outside the `if (ACTIVE)` block. If rule-specific error messages were added in Step 5, use the specific constant instead of `INVARIANT_BREAK`.

---

## Step 5 — Add error message constant (optional)

If the generic `INVARIANT_BREAK` message is not descriptive enough for this rule, add a specific constant to `QuizzesErrorMessage.java`:

```java
CANNOT_<OPERATION>_WHEN_<CONDITION>("Cannot <operation>: <human-readable reason>"),
```

Only add a new constant if the rule has a distinct business meaning that should surface clearly in error logs and tests.

---

## Step 6 — Update the `INTRA-INVARIANTS` comment block

At the top of the aggregate class there is a block like:

```java
/*
    INTRA-INVARIANTS
        RULE_A
        RULE_B
    INTER-INVARIANTS
        ...
*/
```

Add the new rule name to the `INTRA-INVARIANTS` section.

---

## Step 7 — Write tests

Add test cases to the relevant `*Test.groovy` under `src/test/groovy/.../sagas/coordination/<aggregate>/`.

Cover at minimum:

| Scenario | Expected outcome |
|----------|-----------------|
| Invariant holds (valid state) | Operation succeeds, aggregate committed |
| Invariant violated | `QuizzesException` thrown at UoW commit (or immediately in `verifyInvariants()`) |
| Edge / boundary cases | Correct behaviour (e.g. exactly-at-limit values) |

Run with:
```bash
mvn clean -Ptest-sagas test -Dtest=<YourTestClass>
```

---

## Checklist before finishing

- [ ] `invariant<RuleName>()` private boolean helper added with `/* RULE_NAME */` doc comment
- [ ] `verifyInvariants()` updated with the new call in the correct state-scoping block
- [ ] Error message constant added if rule-specific (otherwise `INVARIANT_BREAK` reused)
- [ ] Class-level `INTRA-INVARIANTS` comment updated with the new rule name
- [ ] Tests written and passing
