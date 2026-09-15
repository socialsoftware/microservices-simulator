# Integral input fix handoff

Status: complete within the approved bounded preparation/qualification scope.

## Changed

- ScenarioCatalogPackageReader.currentScalar preserves Jackson integral Number values;
  Java's int/long conditional promotion no longer boxes every integral scalar as Long.
- ScenarioMaterializer.assignmentValue binds DTO setter/field assignments to the declared
  integral width exactly. It rejects fractions and overflow; application code is unchanged.
- ScenarioExecutorSpec adds persisted numeric boundary and dummyapp DTO assignment
  regressions, a Long destination and negative conversion cases. The existing overload
  fallback fixture now uses a genuinely wide Long instead of relying on the reader bug.
- Canonical current-state/roadmap and linked retained evidence describe the actual outcome.
  Previous dirty changes, including generic class loading, were preserved.

## Proof and outcome

273 executor/package/artifact tests pass. The meaningful pre-fix regression had six
failures: four small integral type cases, a beyond-Long value and dummyapp DTO assignment.
The original failed-control package now materializes and starts all three participants.
Its no-fault execution reaches an application invariant rejection after the creator is
anonymized: PARTIAL_COMPENSATED/DEVIATED, four-criterion score zero. This is an application
execution result, not a remaining preparation failure.

A predeclared seed-15156 sample of 12 unique variants completes with 12 available zero
scores (nine PARTIAL_COMPENSATED and three COMPENSATED). It does not establish that every
one of the 156 alternatives is zero. The earlier generated lost-copy positive still gives
SUCCESS/EXACT, I=0, read A=0 and lost-copy count 1 with complete coverage. Report hashes,
score recomputation and candidate uniqueness were independently verified; runtime hashes
remain intact. Fourteen fresh application attempts total: original control, twelve sample
cases and the cross-detector regression.

Sources, class overlays, config and raw outputs are retained under
verifiers/target/integral-input-materialization-01/. The evidence summary is at
docs/verifiers-impl/evidence/integral-input-materialization-2026-09-15/README.md.

No GA campaign, paper change, commit, branch or application business-code change. The next
cohort decision should use the observed lack of variation in this sample rather than
assuming that unlocking materialization guarantees an informative benchmark.
