# Lost copied update qualification

The [retained results](../../../docs/verifiers-impl/evidence/lost-copied-update-2026-09-15/README.md)
explain the domain histories, findings, limitations and measured costs.

- `qualify.py` runs five controlled histories in each local transport mode using the
  integrated constructor observer and assessor. `ExtractContracts.java` extracts the
  contracts through the production source visitor.
- `validate.py` compares their business observations and committed versions with the
  original uninstrumented baseline.
- `measure.py` runs two fresh-process enabled/disabled timing pairs. These include startup;
  they are not an isolated observer-overhead benchmark.
- [M3-BOUNDED.md](M3-BOUNDED.md) gives the source-derived generation and ordinary executor
  commands. `m3-bounded.json` retains the three accepted case selections.

These helpers rely on the named frozen local artifacts under `verifiers/target/`; they
are reproducibility tools for this bounded qualification, not a clean-checkout bootstrap.
For normal application execution, the existing Compose `scenario-executor` launcher accepts
`LOST_COPIED_UPDATE=true` and builds the matching application and observer. Reports retain
findings and coverage independently of fitness weights. The optional five-criterion search
policy is `weighted-criteria-v2`; all five weights must be explicit.
