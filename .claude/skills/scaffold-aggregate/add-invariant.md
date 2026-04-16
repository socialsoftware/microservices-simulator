# Add Intra-Invariant

These instructions add one Layer 1 intra-invariant to an existing aggregate. The aggregate name
and rule name are provided by the caller. Follow these steps once per rule.

An intra-invariant is a consistency rule that must always hold within a single aggregate instance.
It is checked by `verifyInvariants()` on every Unit of Work commit.

> Do NOT throw domain exceptions from mutation methods — all aggregate state rules belong in
> `verifyInvariants()`. If this rule depends on state from another aggregate, it belongs in
> Layer 4 (`/wire-event`) instead.

---

## Step 0 — Understand the rule

- **AggregateName** and **rule name** (e.g., `COURSE_NAME_FINAL`) are given by the caller.
- Derive the rule's precise meaning from the name. When the name is ambiguous, read the domain
  model template for the formal rule statement.
- Decide **state scope**: does the rule apply to ALL states, ACTIVE only, or another subset?
  - Rules involving business-logic fields → ACTIVE only
  - Rules involving structural integrity → all states
- **Important**: if the rule is enforced at the Java language level (e.g., `final` field, `@Column(nullable=false)`) and cannot be violated at runtime, document this in the class-level comment block and skip Steps 2–5. No `boolean` helper needed.

---

## Step 1 — Read existing code

Before writing anything, read:
1. The aggregate class `microservices/<aggregate>/aggregate/<Aggregate>.java` — focus on:
   - The class-level `INTRA-INVARIANTS` comment block
   - Existing `boolean invariantXxx()` helpers
   - The `verifyInvariants()` method and its state-scoping blocks
2. `<App>ErrorMessage.java` — existing error constants

---

## Step 2 — Add the private boolean helper

```java
/*
 * RULE_NAME
 * <formal statement of the rule>
 */
private boolean invariantRuleName() {
    return <condition that is true when invariant holds>;
}
```

---

## Step 3 — Call from `verifyInvariants()`

Add the call in the correct state-scoping block:

```java
@Override
public void verifyInvariants() {
    if (getState() == AggregateState.ACTIVE) {
        if (!(invariantExistingRule()
                && invariantRuleName())) {   // ← add here
            throw new <App>Exception(INVARIANT_BREAK, getAggregateId());
        }
    }
}
```

If the rule applies to all states, add it outside the `if (ACTIVE)` block.
If a rule-specific error constant is more descriptive than `INVARIANT_BREAK`, use it (see Step 4).

---

## Step 4 — Add error message constant (optional)

Only if the generic `INVARIANT_BREAK` is not descriptive enough:

```java
CANNOT_<OPERATION>_WHEN_<CONDITION>("Cannot <operation>: <human-readable reason>"),
```

---

## Step 5 — Update the `INTRA-INVARIANTS` comment block

At the top of the aggregate class, add the new rule name to the `INTRA-INVARIANTS` section:

```java
/*
    INTRA-INVARIANTS
        EXISTING_RULE
        RULE_NAME        ← add here
    INTER-INVARIANTS
        ...
*/
```

---

## Checklist (per rule)

- [ ] `invariant<RuleName>()` private boolean helper added (or documented as compile-time guarantee)
- [ ] `verifyInvariants()` updated with the new call in the correct state-scoping block
- [ ] Error message constant added if rule-specific (otherwise `INVARIANT_BREAK` reused)
- [ ] Class-level `INTRA-INVARIANTS` comment updated with the new rule name
