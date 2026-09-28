# Experimental order context and hybrid LinUCB

`order_features.py` and `hybrid_linucb.py` are opt-in research helpers. They do not add
allocator policy labels, change recorded/live loaders, or change GA/reward behavior.

Order extraction consumes the persisted WorkloadPlan schedule plus co-generated
`sagas.jsonl` access records. It computes seven bounded counts: cross-participant
read-before-write, write-before-read, write-before-write, reads between foreign writes,
and the three corresponding patterns involving a mapped event receiver access. Matching
uses aggregate type only. Every name therefore says `type-potential`; no coordinate joins
object identity or claims an anomaly. Interaction evidence does not gate these features,
so `typeOnly` inputs remain usable context. Missing or ambiguous Saga/step/access joins
appear in `coverage.gaps` and strict extraction raises `OrderFeatureCoverageError`.
Known analyzer limitations remain in `coverage.limitations` while already extracted
accesses remain usable. A zero coordinate means only that no such access was recorded.

Use `load_compatible_structural_metadata(interaction_files)` to verify each interaction
file and its sibling Saga file against their adjacent package manifest and to require one
exact structural source fingerprint. The frozen experiment must also pin the Saga and
manifest hashes itself. The manifest establishes that the two roles were co-generated;
an unpinned adjacent manifest is not an independent integrity anchor.

```python
metadata = load_compatible_structural_metadata(interaction_files)
orders = {w['id']: order_profile(w['domain'].workload, metadata) for w in workloads}
augmented = augment_profiles({w['id']: w['profile'] for w in workloads}, orders)
space = OrderFeatureSpace(augmented)
for workload in workloads:
    workload['profile'] = augmented[workload['id']]
policy = ContextualLinUcbPolicy(space, exploration=1.0, ridge=1.0)
```

`HybridLinUcbPolicy` has the existing `select(active, states)`,
`update(workload_id, context, reward)`, and `summary()` shape. For the original-information
hybrid comparison, build the shared space with `StructuralFeatureSpace(original_profiles)`
and use the existing `ProgressFeatureSpace()` as the per-workload space. Both contain a
bias: one learns a shared intercept and the other allows workload-specific evidence to
shift it. The partition preserves the original structure-plus-progress information basis;
it does not add order features. Null reward does not update either parameter block.

The implementation follows hybrid LinUCB in [Li et al., WWW 2010](https://archives.iw3c2.org/www2010/_lihong/pub/Li10Contextual.pdf).
Its Schur-complement recursive update is equivalent to one direct ridge system containing
the shared block and every workload's local block, while avoiding that large dense matrix.
The tests compare coefficients, predictions, and confidence terms with a direct block
solve at ridge 1 and alpha 1.

The retained Quizzes Saga metadata exposes command-level analyzer footprints. A
`SagaCommand` write can reflect locking/protocol behavior rather than a business update,
so feature names describe extracted access modes only. The retained source also reports
unresolved dispatch/payload limitations; experiments must retain those coverage gaps and
must not interpret an absent access as a proved absence.

The metadata-only availability audit over the frozen 81-workload transfer inventory found
no missing Saga/step/access joins. Sixteen scheduled uses carry the retained unresolved
`UpdateTournament.updateQuizStep#0` dispatch limitation, including ten of the 15 target
workloads; their supported coordinates remain available with `staticKnowledge: PARTIAL`.
The target inventory has six original structural profiles and eight order vectors. In the
ten-workload originally identical profile, ordinary order yields five vectors. The audit
did not read candidate outcomes. All 17 retained package copies declare the same Saga hash
`03c8aca1…dda98` and interaction hash `69cd479e…6e6c0f`; the experiment input must retain
their full hashes rather than these display abbreviations.
