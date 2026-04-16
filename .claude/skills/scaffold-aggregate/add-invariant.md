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

Add the call in the correct state-scoping block with its own `if` statement:

```java
@Override
public void verifyInvariants() {
    if (getState() == AggregateState.ACTIVE) {
        if (!invariantExistingRule()) {
            throw new <App>Exception(EXISTING_RULE_ERROR);
        }
        if (!invariantRuleName()) {      // ← add here
            throw new <App>Exception(RULE_NAME_ERROR);
        }
    }
}
```

If the rule applies to all states, add it outside the `if (ACTIVE)` block but still with its own inner `if`.

> **Each invariant gets its own `if` block.** This makes it immediately clear which invariant was violated when an exception is thrown. Use the most descriptive specific error constant available (e.g., `COURSE_MISSING_NAME`); use `INVARIANT_BREAK` only as a last resort if no domain-specific constant fits. Group invariants sharing the same state scope under the same outer `if (getState() == ...)` guard, but each gets its own inner `if`.

---

## Step 4 — Add error message constant

Add a domain-specific error constant to `<App>ErrorMessage.java`:

```java
RULE_NAME_ERROR("Human-readable message explaining the invariant violation"),
```

Prefer a specific constant per invariant — it should explain what went wrong in domain terms.
`INVARIANT_BREAK` is now reserved for cases where no specific constant is appropriate.

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
