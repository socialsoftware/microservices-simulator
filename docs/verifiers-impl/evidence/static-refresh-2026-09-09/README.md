# Static candidate refresh — 9 September 2026

This refresh compares the retained verifier classes with the final qualified
source-input overlay against one frozen Quizzes source tree. It measures static
preparation capability. It does not execute scenarios, prove runtime success, detect an
anomaly, or score impact.

## Comparable result

An ordinary input or selected input tuple is a candidate when it has a materializable
source setup, or its extracted recipe is directly materializable without setup. Provider
and event additions are excluded from this numerator.

| Campaign | Baseline source setup | Current source setup | Direct/no setup | Blocked | Gain/loss |
|---|---:|---:|---:|---:|---:|
| All ordinary singles, max 1,000 inputs/Saga | 625 | 681 | 53 | 131 → 75 | 56 / 0 |
| Bounded sizes 1–3, max 10 inputs/Saga | 274 | 311 | 10 | 119 → 82 | 37 / 0 |

The refreshed singles result is therefore **734/809** static candidates, up from
**678/809** in the same-source baseline. All 809 exact selected input IDs and the entire
`inputs.jsonl` are byte-identical across the A/B comparison. The 56 newly preparable
inputs are 36 CreateTournament, 18 UpdateTournament, one CreateTournamentAsync and one
UpdateQuestionTopicsAsync. Seventeen use the newly preserved ordered ACTION_RESULT
field assignments; the other 39 gain setup through the approved nested result
projections. No accepted or rejected input recipe changed in the comparison.

The broader campaign retains the existing space-map boundary: include singles, Saga
set size through three, ten inputs per Saga, SERIAL/one schedule, up to three event
consequences, 50,000 catalogue limit and recovery cap one. The structural accounting
found 7,806 Saga sets and 1,550,127 input-bound possibilities; the bounded selection
contains 70 Saga sets and 403 base input tuples. Its 37 gains comprise 19 singles, 14
pairs and four triples. The generated package contains the same 3,820 semantic workload
records before and after. Source setup makes more of them useful, increasing written
fault scenarios from 12,395 to 22,542; this is static enumeration, not execution proof.

Setup changes legitimately alter deterministic IDs. In singles, 56 workload IDs change;
in the broad package, 997 change. Removing `id` and `setup` from each workload produces
an identical multiset in both comparisons: zero semantic workload gains or losses. The
report therefore attributes gains by exact participant input IDs and source
call/occurrence rather than treating ID churn as coverage.

## Historical 665/796 result

The retained September 5 package reported 665 candidates from 796 ordinary accepted
inputs. It is not the A/B baseline because its application test snapshot differs. Its
raw input file contains 800 accepted rows, including four which the campaign input policy
does not select. The refreshed raw file contains 813; exact and semantic comparison finds
798 common, 15 added and two removed source calls, with no same-semantic ID churn. This
explains the net denominator increase from 796 to 809 as fixture/source drift. The
headline change from 665/796 to 734/809 combines that drift with verifier capability and
must not be called a 69-input implementation gain. The controlled implementation gain is
56/809.

The campaign used the prepared Quizzes tree under
`verifiers/target/empty-event-delivery/run-01/prepared-build/source/quizzes`. Against the
checkout, all 510 shared Java/Groovy files are byte-identical; the only path difference is
the prepared tree's extra `QuizzesImpactV2PrerequisiteProvider.java`, an executor
experiment provider. Two additional current-overlay generations read the actual checkout
application tree. For both campaign configurations, all nine package files are
byte-identical to their frozen-source counterparts, including inputs, setups, workloads,
fault scenarios and accounting. The prepared-only provider has no artifact effect here.

## Reproduction and evidence

From the repository root, the complete campaign command is:

```sh
refresh=verifiers/target/static-refresh-reproduction
STATIC_REFRESH_OUTPUT="$PWD/$refresh" bash verifiers/target/static-refresh-2026-09-09/run-campaign.sh all
STATIC_REFRESH_OUTPUT="$PWD/$refresh" python3 verifiers/target/static-refresh-2026-09-09/analyze.py
STATIC_REFRESH_OUTPUT="$PWD/$refresh" python3 verifiers/target/static-refresh-2026-09-09/provenance.py
```

The four generator runs exited zero in 19–33 seconds. They use Docker image
`sha256:0aab59d58bfe4f83e6bee2a4002a913dcbc26d861acee5f0327c053f12918282`,
network disabled, the retained prepared build, and the 12-file overlay declared by
`issues/2026-09-09-source-update-read-inputs/source-overlay.json`. The overlay hashes
match the issue's final-generation provenance. The raw packages, logs and exact commands
are under `verifiers/target/static-refresh-2026-09-09/`. Its `comparison.json` contains
the exact gained source identities and accounting; `provenance.json` contains source,
class, image, command, package and log hashes; `gained-single-inputs.json` and
`gained-broad-tuples.json` retain the detailed gains.

The two checkout-source controls also exited zero (20 and 28 seconds). The reproduction
command above creates the four attributable A/B packages; add
`current-checkout-singles` and `current-checkout-broad` as runner arguments to repeat the
source controls.

This bounded broad run completed normally. It does not resolve the separate expensive
`InputTupleSelection.countGroups` case with size three and 1,000 inputs per Saga, and it
does not claim exhaustive pair/triple coverage. HashSet constructor reconstruction also
remains outside the qualified capability.

Two harness bootstrap attempts produced no package and are excluded from measurements.
The first tried the retained `/reports/...` classpath on the host, so `javac` could not
resolve dependencies. The second ran in Docker but put Quizzes application classes ahead
of verifier classes, causing Spring to request a servlet server. The retained harness uses
Docker paths and separates the larger compile classpath from the verifier-only generator
runtime classpath.
