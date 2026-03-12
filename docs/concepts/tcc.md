# TCC (Try-Confirm-Cancel / Causal)

## What It Is

TCC (implemented here as "Causal") handles concurrent writes to the same aggregate by **merging** conflicting versions at commit time, rather than using semantic locks. Two concurrent updates to different fields of the same aggregate can both succeed; conflicting updates to fields declared in the same **intention** abort.

## Key Classes

| Class | Location | Role |
|-------|---------|------|
| `CausalAggregate` | `simulator/.../ms/causal/aggregate/CausalAggregate.java` | Interface adding merge logic |
| `CausalUnitOfWork` | `simulator/.../ms/causal/unitOfWork/CausalUnitOfWork.java` | Tracks causal version snapshots |
| `CausalUnitOfWorkService` | `simulator/.../ms/causal/unitOfWork/CausalUnitOfWorkService.java` | Creates and commits/aborts `CausalUnitOfWork` |
| `CausalWorkflow` | `simulator/.../ms/causal/workflow/CausalWorkflow.java` | Workflow engine for TCC |

## CausalAggregate Interface

Three methods every TCC class must implement:

```java
// Names of fields that may be modified concurrently
Set<String> getMutableFields();

// Pairs of fields that must change together (conflict detection)
// If concurrent tx changes field[0], another concurrent tx must not change field[1] alone
Set<String[]> getIntentions();

// Field-level merge: given which fields changed in each version, return the merged aggregate
Aggregate mergeFields(Set<String> toCommitVersionChangedFields,
                      Aggregate committedVersion,
                      Set<String> committedVersionChangedFields);
```

## How Merge Works

At commit time, if a newer version of the same aggregate was committed concurrently:

1. `getChangedFields(prev, toCommitVersion)` — fields changed by this transaction
2. `getChangedFields(prev, committedVersion)` — fields changed by the concurrent transaction
3. `checkIntentions(...)` — throw if any intention pair is split across the two change sets
4. `mergeFields(...)` — build the merged version (usually: take each changed field from whichever tx changed it; if both changed the same field, it is a conflict)

## TCC Class Hierarchy

```
Aggregate (abstract)
  └── Execution (abstract, in quizzes)
        └── CausalExecution implements CausalAggregate
```

`CausalExecution` implements `getMutableFields()`, `getIntentions()`, and `mergeFields()`.

## Mutable Fields & Intentions Example

```java
@Override
public Set<String> getMutableFields() {
    return new HashSet<>(Arrays.asList("acronym", "academicTerm", "endDate", "courseQuestionCount"));
}

@Override
public Set<String[]> getIntentions() {
    return new HashSet<>();  // no field pairs that must change together
}

@Override
public Aggregate mergeFields(Set<String> toCommitVersionChangedFields,
                              Aggregate committedVersion,
                              Set<String> committedVersionChangedFields) {
    CausalExecution merged = new CausalExecution((CausalExecution) committedVersion);
    if (toCommitVersionChangedFields.contains("acronym")) {
        merged.setAcronym(this.getAcronym());
    }
    // ... repeat for each mutable field
    return merged;
}
```

## Naming Conventions

| Layer | Pattern | Example |
|-------|---------|---------|
| TCC class | `CausalXxx` | `CausalExecution` |
| TCC factory | `CausalXxxFactory` | `CausalExecutionFactory` |
| TCC repository | `XxxCustomRepositoryTCC` | `CourseExecutionCustomRepositoryTCC` |
| Functionality | `XxxFunctionalityTCC` | `UpdateCourseQuestionCountFunctionalityTCC` |

## Reference Implementation

- `applications/.../execution/aggregate/causal/CausalExecution.java` — mutable fields, intentions, mergeFields
- `applications/.../execution/coordination/causal/UpdateCourseQuestionCountFunctionalityTCC.java` — simple single-step TCC functionality
