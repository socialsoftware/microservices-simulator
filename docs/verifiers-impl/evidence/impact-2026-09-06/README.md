# Evidence retained for the impact discussion

These files are exact copies of the completed qualification reports, retained outside
Maven's disposable `target/` directory. `provenance.json` records their original paths and
SHA-256 hashes. Internal attempt, package and source paths are intentionally unchanged.

- `remove-course-control-*`: ordinary successful CourseExecution removal.
- `remove-course-late-fault-*`: the same selected workload, failing before removal after
  the Course count has already changed. Execution and ImpactV2 reports are separate.
- `qualification-summary.json`: the final 30 broader pairs, with selected-run provenance;
  also accounts for all 93 retained attempts, including the two invalid preparation trials.
- `benchmark-comparison.json`: 29 corrected benchmark assessments and comparison with
  the earlier partial observer results.
- `unaffected-selection-comparison.json`: verifies executable descriptions retained
  across the final prerequisite repair; opaque SolveQuiz IDs changed without changing
  executable inputs, preparation or actions.

This is a small evidence extract, not a replacement for the full reproduction package,
logs or frozen builds. Full reports and packages remain at their recorded locations;
the meeting note only claims what these reports and the linked application code support.
