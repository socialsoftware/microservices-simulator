# Lost copied updates without hand-written mappings

## Question and result

Can the tool establish the copied-value overwrite chain without supplying the relation
between Quizzes DTO fields, command inputs and persisted fields by hand?

**Yes, for the direct-constructor-copy pattern exercised here.** Source analysis infers
nine Quizzes copy contracts, including `TopicDto.name -> TournamentTopic.topicName` and
the collection identity relation `aggregateId -> topicAggregateId`. A Java agent then
records the real response, outgoing input, transport pairing, executed constructor,
aggregate registration and matching committed write. No Quizzes source was changed.

The application source parsed by the extractor is byte-for-byte equal to the source of
the frozen runtime build. The source pattern and runtime observer have no Quizzes class
or field-name checks. `aggregateId` is an existing simulator identity convention.

## Histories

The same [five application histories](../stale-write-2026-09-15/README.md) run with local
serialization disabled and enabled, each in a fresh JVM/H2 database. A is the Tournament
update Saga. B renames a Topic; its event triggers C, which updates the Tournament's local
copy of that Topic. Dates, setup, selected receiver and fault boundary remain fixed.

| History | Relevant order | Findings in each transport mode |
| --- | --- | --- |
| Forward stale | A reads old Topic; B/C rename its persisted copy; A copies its earlier input | 1 copied-name overwrite, FORWARD |
| Forward fresh | B/C rename before A reads the Topic | 0 |
| Recovery stale | A saves original Tournament; B/C rename its copy; fault before Quiz update; A compensates with saved DTO | 1 copied-name overwrite, RECOVERY |
| Recovery delayed event | B renames; A compensates; C applies the event afterwards | 0 |
| Recovery without event | A compensates with no intervening rename | 0 |

Result: **4 positive executions, 6 controls**, with no recorded observer or assessed-path
coverage gaps. Each positive identifies exactly the copied Topic name in Tournament 11,
Topic 4 in this fixture. The generic assessor never consumes these expected IDs or names.
The source Topic remains renamed; the overwritten value is the Tournament's local copy.

For a serialized forward positive, the retained chain identifies a response carrying
`TOPIC 1`, an input carrying that same observed value, the actual copy constructor, C's
committed Tournament version 23 carrying `RENAMED TOPIC`, and A's committed version 24
carrying `TOPIC 1`. For the recovery positive, the corresponding overwrite is version
24 -> 25 and the origin is a nested Topic in the earlier Tournament response.

## Controls and validation

- Dummyapp `CardInput.caption -> StoredCard.heading` is inferred despite unrelated names.
  A computed `toUpperCase()` replacement is excluded by the supported source pattern.
- Ten Spock cases pass (JUnit reports 13 successful tests including parameterized feature
  nodes). They cover the same instance, untracked clone, changed input, transport clone,
  reordered collection, duplicated alias, mismatched transport value, duplicate identity,
  and a constructed object that is never registered/persisted.
- Five evidence-removal checks confirm that removing responses, command inputs,
  constructor observations, aggregate registration or committed writes prevents positives.
- Initial/final application observations, producer outcomes, committed identities,
  versions, writers and application data match the original ten uninstrumented histories.
  Only top-level `creationDate` and `lastModifiedTime` are excluded from the committed-data
  comparison: these application audit timestamps use wall time across fresh processes.
- The final run has no recorded probe failures. Early development runs exposed enum
  subclasses and transport duplication of shared DTO aliases; both are handled in the
  final observer. Source extraction alone did not prove these runtime details.

## Artifacts and boundaries

- Raw run: `verifiers/target/inferred-stale-write/run-05/`.
- Retained summary: [validation.json](validation.json).
- Inferred application contracts: [contracts.json](contracts.json).
- Reproduction and implementation: [experiment README](../../../../verifiers/experiments/inferred-stale-write/README.md).
- Raw directory retains source, application-source hashes, compiled agent/classes,
  command lines, original reports, `.trace.json` histories, fixture controls, Spock output,
  validation and an artifact hash index. Final analysis scripts are in `analysis-source/`;
  `source/` preserves the files frozen when the application executions were prepared.

This is a feasibility proof using controlled histories and the existing test setup. It
does not automatically discover those schedules, qualify ordinary generated workloads,
cover arbitrary transformations, or change the production detector/ImpactV2/GA fitness.
The next step is integrating this supported evidence chain into ordinary execution,
preserving explicit coverage and using source-derived test setup. General-purpose runtime
taint tracking and an exhaustive lost-update detector are outside this proof.
