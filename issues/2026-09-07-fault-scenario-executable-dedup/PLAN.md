# Execution plan

1. Characterize the writer/reader projection mismatch with a failing dummyapp-style
   service fixture: equal compact actions, unequal regenerated IDs, and one unwanted add.
2. At the on-demand merge boundary, index validated persisted fault records by their exact
   compact executable content excluding `id`; retain ID collision checks and reuse the
   lowest retained ID for pre-existing duplicates.
3. Cover eager-then-demand, repeat request, historical duplicates, distinct recovery
   orders, participant/slot order and event order. Run the focused service and package
   contract tests under Java 21 from an isolated source copy and private Maven cache.
4. Request the observed w07 all-zero vector against a copied compatible package; prove
   zero added scenarios, the retained eager ID, package validity and unchanged originals.
5. Update canonical behavior/roadmap and write the final proof handoff. Review with Astra
   at medium effort, resolve blocking findings, then commit only this task's files locally.
