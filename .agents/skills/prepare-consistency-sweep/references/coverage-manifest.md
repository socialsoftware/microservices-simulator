# Coverage manifest

Write `src/test/resources/consistency-testing/catalog-coverage.yaml`. Keep entries stable and reviewable.

## Required shape

```yaml
schema_version: 1
application: Example
mode: neutral-baseline
saga_aggregates:
  - class: com.example.aggregate.sagas.SagaItem
    source: src/main/java/com/example/aggregate/sagas/SagaItem.java
    catalogs: [default]
    handles: [item]
aggregate_relationships:
  - from: SagaItem
    to: SagaCollection
    relation: item references its collection
    evidence:
      - src/main/java/com/example/aggregate/Item.java
functionalities:
  - class: com.example.CreateItemFunctionalitySagas
    kind: write
    included: true
    catalogs: [default]
    functionality_ids: [createItem]
    evidence:
      - src/test/java/com/example/ItemTest.java
    exclusion_reason: null
candidate_inter_invariants:
  - description: Item names may be unique within one collection.
    evidence:
      - src/main/java/com/example/ItemService.java
    status: candidate-only
confirmed_inter_invariants: []
unimplemented_explicit_inter_invariants: []
cross_catalog_gaps: []
```

## Rules

- One entry per discovered class, even when excluded.
- Record every discovered saga aggregate, including aggregates not represented by a current handle.
- Record only aggregate relationships explicit in code or docs. Do not infer likely races from them.
- `kind` describes operation intent, not every observed access: `read` has no intended state change; `write` intends mutation or state-changing event even when it reads preconditions; `mixed` has ordinary valid inputs producing either read-only or mutating behavior; `unknown` lacks evidence.
- List every catalog and ID when same class has multiple concrete instances.
- `included: false` requires nonblank `exclusion_reason`.
- Evidence paths are repository-relative and point to code, test, or docs used for decision.
- Candidate invariant status is always `candidate-only`; no generated invariant code.
- Confirmed invariants cite explicit source and generated provider method.
- Every explicit cross-aggregate invariant is either confirmed or listed under `unimplemented_explicit_inter_invariants` with evidence and reason.
- `cross_catalog_gaps` names functionality pairs that cannot be explored because they need different catalogs.
- Keep `mode: neutral-baseline`. This is required for current coverage manifests. No hypothesis-driven or targeted-enrichment mode is defined yet; defer it until thesis methodology and bias controls are specified.
