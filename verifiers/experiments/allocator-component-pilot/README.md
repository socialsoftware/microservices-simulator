# Quizzes allocator component design pilot

The opt-in policies in `policies.py` add three missing component ablations without
changing the allocator CLI or live dispatcher:

- `S-shared`: original structural features, no observed-progress coordinates;
- `P-local`: separate progress LinUCB coefficients for every workload;
- `H0`: one shared bias and local progress coefficients, with the same local
  vector as the existing hybrid (`H1`).

`S-unit`, `SP-unit`, and `H1-unit` were exploratory scale controls. They also
scaled the bias, so they are not the recommended matched comparison. `S-cal`,
`SP-cal`, and `H1-cal` keep bias at one, give the other structural coordinates
unit norm, and set exploration analytically to equalize the initial uncertainty
within P/S/SP and H0/H1. The old raw and `unit` runs are retained as diagnostic
history. Matching the initial uncertainty does not make later updates identical.

Run the short recorded-feedback replay:

```sh
python3 verifiers/experiments/allocator-component-pilot/run_pilot.py \
  --seed 1 --budget 32 \
  --output verifiers/target/allocator-component-pilot-2026-09-23/seed-1-budget-32-traces.json
python3 -m unittest discover -s verifiers/experiments/allocator-component-pilot -p 'test_*.py' -v
```

The runner uses the already examined 66- and 15-workload Quizzes panels, two
existing reward profiles, and the same local GA and recorded-feedback evaluator
as earlier experiments. It keeps decision-level choices, estimates, uncertainty,
pre-feedback progress and observed scores in its JSON output. It does not execute
Quizzes or treat pilot scores as a confirmatory result.
