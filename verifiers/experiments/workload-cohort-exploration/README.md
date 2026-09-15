# Fixed-workload cohort exploration helpers

These helpers support the 2026-09-15 workload-size exploration. They do not
change the verifier, executor, detector, or search implementation.

`count_domain.py` prepares an isolated copy of a scenario package, submits one
ordinary on-demand generation request for every canonical fault vector, and
writes `domain-count.json`. Its candidate count is exact only when
`countComplete` is true and `anyTruncated` is false. Run it with:

```sh
python3 verifiers/experiments/workload-cohort-exploration/count_domain.py \
  --config /absolute/path/to/config.json \
  --output /absolute/new/output/directory
```

`GenerateSelectedInputPackage.java` is a retained exploratory source scanner.
It accepts an exact comma-separated set of source-derived input IDs, requires
one input per distinct Saga, and keeps only workloads with source setup, no
manual prerequisite provider, and positive static materializability. The
failed generation trials are evidence of the current setup-cohort boundary;
the helper is not part of the production generation path.

For a newly added normal application test whose deterministic IDs are not yet
in a package, its fourth argument also accepts
`story:<class-suffix>|<method>|<SagaSimpleName+...>`. The selector still
requires exactly one source-derived input per requested Saga and records the
resolved IDs in `generation-proof.json`.

An optional eleventh argument bounds the number of exact selected-input
workloads retained after generation. The generator's earlier global workload
and schedule caps still apply and are reported separately; a retained cap does
not establish that later schedules were explored.

An optional twelfth argument selects schedules with whole-participant ordering
constraints after ordinary generation and materializability checks. Write, for
example, `SagaA<SagaB+SagaB<SagaC`; every forward step of the saga on the left
must precede every forward step of the saga on the right. This is an explicit
exploration filter, and `generation-proof.json` records the constraints and all
earlier generator caps.

`make_config.py` derives a small experiment configuration from an existing
configuration while preserving its complete frozen runtime descriptor. This
avoids duplicating or manually editing the large runtime hash map when probing
several fixed workloads.

`qualify_vectors.py` resolves a declared `LABEL=VECTOR[:RECOVERY_INDEX]`
case list through the ordinary on-demand generator, then runs each selected
persisted scenario in a fresh container. It is intended for short structural
qualification before a count or sample, and retains the request and attempt
artifacts used for every case.
