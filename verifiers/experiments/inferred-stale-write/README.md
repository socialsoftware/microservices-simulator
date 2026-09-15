# Automatic copy-origin proof

This experiment extracts direct field copies from application source and observes their
execution, without hand-written Quizzes DTO/command/persistence mappings. It uses the
controlled histories and application setup of `../stale-write`; it does not automatically
generate these histories or integrate a production lost-update criterion.

## Mechanism

`ExtractCopies.java` parses all supplied application Java source. It recognises a narrow
pattern: an entity's single-argument constructor invokes direct setters with direct
getters from that argument. Getter and setter bodies must resolve to scalar fields. The
simulator `aggregateId` convention supplies the source identity; the destination key
field is inferred from its assignment. Computed values, unresolved accessors and classes
without this identity relationship are outside the pattern.

`Agent.java` instruments the inferred constructors and three framework boundaries using
the existing Byte Buddy dependency. No application source is rewritten. `Probe.java`:

1. Records supported objects in actual gateway responses, including nested objects.
2. Captures the outgoing command graph and existing object origins before serialization.
3. Bridges the same synchronous local call to its received command, checking types,
   keyed paths and projected values before assigning any origins. Collection order and
   alias duplication may change across transport. Equal values outside this transport
   pairing do not create an origin.
4. Records actual constructor invocations using admitted command-input objects. Checks
   copied values still equal the original response values and constructor output fields.
5. Finds those exact constructed instances in an aggregate passed to `registerChanged`.
6. Joins the registered identity/version/writer with transaction-confirmed observations.

`assess.py` joins this chain with an intervening foreign committed write to the same
persisted field. It requires the foreign write to have changed the old value and the
later write to replace it with the copied value. Identity/version metadata are excluded.
New collection entries with no prior cell are not overwrites. The assessor contains no
Quizzes class names, field names, expected strings or case labels.

The static inference need not reconstruct the whole service call graph: runtime object
identity connects the executed constructor result to the registered aggregate. The
experiment keeps the existing committed-write observer as the persistence authority.

## Reproduce

Requires the existing frozen Quizzes build and read-scope overlay verified by
`fixed-workload-ga/qualify.py`. Docker supplies the matching Java runtime.

```bash
python3 verifiers/experiments/inferred-stale-write/run.py \
  --output verifiers/target/inferred-stale-write/my-run --prepare-only
python3 verifiers/experiments/inferred-stale-write/qualify.py \
  --run verifiers/target/inferred-stale-write/my-run
python3 verifiers/experiments/inferred-stale-write/run.py \
  --output verifiers/target/inferred-stale-write/my-run --reuse-prepared
python3 verifiers/experiments/inferred-stale-write/assess.py \
  --run verifiers/target/inferred-stale-write/my-run
python3 verifiers/experiments/inferred-stale-write/validate.py \
  --run verifiers/target/inferred-stale-write/my-run \
  --baseline-run verifiers/target/stale-write/run-01
```

The ten Quizzes executions are five histories with local serialization off/on. The
source-only dummyapp fixtures exercise unrelated class and field names. The opt-in Spock
spec runs via the prepared runtime's JUnit launcher, including identity loss, changed
inputs, transport mismatch, duplicate keys, reordered collections and duplicated aliases.

## Limits

- This is a sequential, controlled, in-process local transport proof. Its mutable object
  indexes are not production-ready for arbitrary concurrent worker threads or remote RPC.
- Reflection is bounded to the supported graph shape; unsupported/ambiguous paths produce
  gaps. Missing origins yield no attributed copy, not a claim that every input was covered.
- Constructors and copied scalar fields are the supported slice. Computations, general
  setter-based updates, arbitrary cloning and other serialization formats need more work.
- Source inference produces nine Quizzes contracts; only the exercised paths are qualified.
- Event handlers still execute the normal application code. The trace does not infer
  programmer intent or prove complete database-level P4/serializability detection.
- The application business state and committed writes match the prior uninstrumented
  histories, excluding the wall-clock audit fields `creationDate` and `lastModifiedTime`.
  No performance or instrumentation-overhead claim follows from this small run.

See [retained evidence](../../../docs/verifiers-impl/evidence/inferred-stale-write-2026-09-15/README.md).
