# Observer-cycle slice handoff

**State:** complete; ready for independent review and integrated qualification.

## Outcome

`PersistentStateObserver` now recognizes a supported JPA owned back-reference only when
the parent declares the inverse `@OneToOne` or `@OneToMany` `mappedBy` relationship,
cascades persistence ownership, and actually contains the child being projected. The
back-link becomes an explicit `OWNED_ANCESTOR` reference with a relative level count.
That reference does not contain generated row identity or a collection traversal index,
so it is stable across revisions and changes in `Set` iteration order.

The participant-answer shape is covered generically: the participant owns its answer
through `mappedBy = "tournamentParticipant"`, and the answer's link back to that
participant can be represented without recursively projecting it. All answer fields
remain in the projection. Unsupported cycles retain `PERSISTENT_MAPPING_CYCLE` and their
diagnostic `cycleRef`; this is not a blanket cycle suppression.

## Actual files changed

- `simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/PersistentStateObserver.java`
- `simulator/src/test/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/impact/PersistentStateObserverTest.java`
- `issues/2026-09-06-impact-coverage-controls/OBSERVER-HANDOFF.md`

## Discovery and decisions

- The previous observer only special-cased a repeated owning `Aggregate`. A repeated
  non-aggregate owned entity produced a coverage gap even when it was the mapped JPA
  back-reference.
- A relative ancestor reference was selected because an absolute path under an unordered
  collection would contain a traversal index and could change equality across JVM loads.
- Generated owned-row IDs remain excluded. Ordered lists and deterministically sorted
  sets keep their previous behavior. Changing an answer's persistent field still changes
  the normalized application data.
- No verifier/dummy fixture was needed because this is wholly a simulator projection
  contract. No scoring, lifecycle, timestamp, dependency, event, or application behavior
  changed. Canonical documentation and runtime qualification remain with the root slice.

## Proof

With JDK 21 (`/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home`):

```text
cd simulator
mvn -Dtest=PersistentStateObserverTest,PersistentStateObserverDatabaseTest test
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The positive test reverses owned entities in a `Set`, changes their generated IDs across
logical revisions, and obtains equal projections with explicit back-references. It then
changes an owned answer count and obtains unequal application data. The negative test
keeps an unowned self-cycle unknown through `PERSISTENT_MAPPING_CYCLE`. The database
precision regression test also passes. `git diff --check` passes for the implementation
and focused test. No Maven install, Docker run, commit, merge, or push was performed.

## Integrated check

Re-run one affected Tournament scenario and verify that the participant-answer path no
longer contributes `PERSISTENT_MAPPING_CYCLE`, while the answer fields and participant
collection still appear in the ImpactV2 snapshots. A reviewer may reasonably veto the
serialized reference field names (`persistentReference`, `kind`, `levels`) before reports
are regenerated; changing those names later would change report equality data.
