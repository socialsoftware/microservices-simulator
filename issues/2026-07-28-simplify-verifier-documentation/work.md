# Make verifier documentation a coherent current handbook

- Workflow: SP coordinator v1
- State: `complete`
- Work shape: `Product`
- Route: `Guided`
- Updated: `2026-07-28`
- Workspace: `/home/andre/microservices-simulator`, branch `fault-analysis/scenarios`
- Git: uncommitted; no commit, push, or PR authorized
- Authority: plan; after implementation approval, edit repository docs/source/tests and create `/home/andre/thesis/meeting-notes/` to relocate the existing verifier meeting-note content
- Implementation approval: `approved (user: "sounds good. go ahead.", 2026-07-28)`

## Current Checkpoint

- Completed: calibrated the canonical reading model, evidence policy, archive policy, decision-record rule, external meeting-note destination, and removal of the obsolete HTML report
- Current: approved outcome delivered and independently reviewed
- Problem: none
- User input: none
- Next: none

## Intent Brief

### Outcome

A user or agent starts at `docs/verifiers-impl/README.md`, follows one deliberate reading order, and can understand the verifier's purpose, current behavior, v3 outputs, latest reproducible evidence, limits, and future direction without searching overlapping glossaries, references, evidence appendices, archives, or meeting notes.

`current-state.md` becomes the expansive canonical verifier handbook. It owns the current conceptual story, essential terminology, supported operations, latest representative evidence, exact reproduction commands and output paths, interpretation, limitations, and safe claims. `roadmap.md` owns detailed future direction without restating current evidence. `README.md` remains navigation.

Normal v3 generation produces exactly the five current contract artifacts. The obsolete pre-v3 HTML/static-trace report surface is removed rather than retained as another optional abstraction.

### Representative Example

Starting only from `docs/verifiers-impl/README.md`, a reader can answer:

> Can the current verifier construct Quizzes Saga inputs, what does the latest `82/82` result prove, how was it obtained, and which output records that proof?

The canonical current-state section identifies the accepted-input and static-candidate context, records the exact setup-preflight command and package/report paths, states `82/82 SETUP_READY`, and explains that this proves exact persisted argument materialization plus Saga startup in that runtime—not successful execution of every all-zero or faulty scenario.

The same reading path explains why the v3 contract has WorkloadPlans, FaultScenarios, a manifest, accounting, and rejected-input diagnostics, while clearly separating machine contract files from preflight, execution, impact, dynamic evidence, and debug logs.

### In Scope

- Reduce the canonical verifier documentation to a deliberate structure centered on:
  - `docs/verifiers-impl/README.md` for navigation;
  - `docs/verifiers-impl/current-state.md` for the expansive current handbook and latest evidence;
  - `docs/verifiers-impl/roadmap.md` for detailed future direction;
  - a small curated `docs/verifiers-impl/decisions/` only for durable active rationale that cannot be stated adequately inline.
- Merge useful, current material from `evidence.md`, the plain explanation, glossary, thesis-claim map, advisor brief, and `reference/` into the appropriate canonical story; delete duplicated, historical, implementation-restating, or stale material rather than carrying it forward.
- Define essential verifier terms at first use in `current-state.md` and update `AGENTS.md`, `CONTEXT-MAP.md`, navigation, and other current repository guidance away from `docs/verifiers-impl/glossary.md`.
- Keep only the latest representative evidence for each current capability. Record the command, relevant configuration, output artifact, discriminating result, interpretation, and limitation. Replace superseded baselines instead of accumulating chronology.
- Keep `roadmap.md` detailed about future outcomes, rationale, dependencies, and completion boundaries, but remove duplicated current-state prose, evidence dumps, and premature algorithm design.
- Audit decision records under the approved rule:
  - retain only active, non-obvious choices likely to matter again;
  - merge short rationale into `current-state.md` where sufficient;
  - consolidate overlapping rationale where one current decision can own it;
  - delete obsolete and superseded records, relying on Git history;
  - link retained decisions from the exact current-state section they explain.
- Create `/home/andre/thesis/meeting-notes/`, copy the 15 existing verifier meeting-note content files there with filenames and bytes preserved, verify the copies, then remove the repository meeting-note directory and its repo-only index.
- Delete the rest of `docs/verifiers-impl/archive/` rather than moving it to another in-repository archive. Git remains the engineering history.
- Remove automatic `analysis-report.html` generation, `AnalysisHtmlReportRenderer`, its dedicated configuration/path surface, dedicated tests, stale docs, and any legacy full human-report logging that has no remaining current consumer.
- Update the verifier documentation-site configuration/script only as necessary so it reflects the reduced canonical set and remains useful; remove redundant site-workflow prose rather than preserving another guide.
- Update focused tests so normal scenario generation proves the exact five-file v3 output boundary.

### Non-goals

- Change the v3 five-file machine contract, WorkloadPlan/FaultScenario semantics, package schemas, materializability policy, execution behavior, dynamic-enrichment behavior, or impact semantics.
- Create or reorganize the broader `/home/andre/thesis/` workspace beyond the authorized `meeting-notes/` directory.
- Move `/home/andre/microservices-simulator` or `/home/andre/thesis-and-paper`, modify the existing Overleaf clones, or define their editing workflows.
- Preserve an in-repository documentation archive after pruning.
- Rewrite historical issue packages or old saved agent sessions merely because they link to removed historical documentation.
- Add a new documentation framework, generated glossary, report abstraction, or compatibility layer.
- Produce `spec.md`, `plan.md`, or additional SP artifacts unless a material discovery makes their distinct coordination job necessary and the scope is revisited.

### Constraints

- The v3 package is the new current contract and remains stable.
- Prefer deletion, consolidation, and inline explanation over new abstractions or directories.
- `current-state.md` may be substantial, but each fact has one owner and its first sections must provide a complete concise mental model before deeper evidence/operation detail.
- Historical evidence does not remain in current docs solely for chronology; Git history and retained external meeting notes provide provenance.
- Current evidence must distinguish static acceptance, static setup candidacy, runtime setup readiness, execution outcome/conformance, and impact.
- Detailed extraction diagnostics and raw evidence may remain machine/debug artifacts, but must not dominate the canonical reader path.
- Preserve unrelated repository work. No commit, push, PR, or broader external filesystem mutation is authorized.

### Acceptance Proof

- Representative outcome: from `README.md` through the declared reading order, a fresh reviewer can answer the representative `82/82` question with the exact command, package/report paths, result, proof boundary, and limitation without opening deleted auxiliary pages.
- Focused regression:
  - run the focused verifier application/output specs after removing HTML generation;
  - verify a normal generated run/test fixture writes exactly the five v3 contract files and no `analysis-report.html`;
  - run the verifier documentation build if the retained site tooling still claims support.
- Material-risk evidence:
  - validate current-state evidence values against the cited JSON artifacts and command/script behavior;
  - verify canonical Markdown and project instructions contain no links to deleted glossary/reference/archive/evidence pages;
  - verify retained decision links resolve and their status/rationale does not conflict with current implementation;
  - obtain one fresh independent SP review of the final documentation, pruning boundary, HTML removal, and proof.
- Boundary checks:
  - verify all 15 external meeting-note copies match their original bytes before deleting repository copies;
  - verify no files outside `/home/andre/thesis/meeting-notes/` were created or changed under `/home/andre/thesis`;
  - inspect final Git status/diff and report the external untracked note destination separately.

## Context and Route

### Current Behavior

- `docs/verifiers-impl/` currently contains 13,766 Markdown lines; `archive/` owns 9,769 lines across 34 files and remains visible to repository search and agents.
- Current truth and evidence are repeated across `current-state.md`, `evidence.md`, `advisor-brief.md`, `verifier-pipeline-plain-explanation.md`, `roadmap.md`, `thesis-claims-evidence-map.md`, `glossary.md`, and four reference pages. These copies have already drifted; older current-path pages still describe ImpactV1 as future work or retain superseded execution interpretation.
- Repository `AGENTS.md` explicitly names `docs/verifiers-impl/glossary.md` as the canonical verifier terminology source, so deleting the glossary requires updating project guidance and inbound links. SP itself has no verifier-specific glossary-path contract.
- Prior-session evidence shows decision records were directly read in at least 25 sessions, especially for catalog, static/dynamic binding, segment-compression, failure-policy, report, and v3 design work. This supports a curated durable-rationale boundary, not automatic retention of every record.
- A current generator run writes six top-level files: the five v3 contract artifacts plus an 8.5 MB `analysis-report.html`. Preflight and execution reports are separate later attempts. The HTML is rendered from `ApplicationAnalysisState` before WorkloadPlan/FaultScenario adaptation and contains no current package, setup, execution, or impact model.
- `/home/andre/thesis-and-paper/{thesis,paper}` already exists. `/home/andre/thesis/` and its proposed `meeting-notes/` directory do not currently exist. This work is authorized to create only the latter meeting-note path.

### Key Invariants

- `current-state.md` is the sole owner of present behavior, essential terminology, latest evidence, reproduction, and interpretation.
- `roadmap.md` owns future direction; decision records own only durable rationale.
- Normal generation retains all five v3 contract artifacts and no legacy HTML report.
- Evidence clearly separates prediction, runtime proof, execution result, and measured impact.
- Meeting-note content is not lost during external relocation.
- Deleted historical material is not replaced by another searchable repository archive.

### Route Rationale

Guided is the minimum safe route because this is a broad but reversible user/agent information-architecture change with one source-output deletion, substantial documentation removal, external local note relocation, and meaningful risk of losing current truth or leaving stale links. One durable intent brief, a planning checkpoint, focused proof, and one fresh independent review are sufficient; separate specs/plans and Governed ceremony are not justified.

### Material Risks

- Over-consolidation could make `current-state.md` another unstructured dump rather than a staged handbook.
- Deleting historical pages may remove the only clear explanation of an active design choice unless rationale is curated before deletion.
- Commands or metrics copied into the canonical handbook may be stale even when existing docs present them as current.
- Removing the HTML renderer may leave hidden constructor/config/test references or oversized legacy text-report logging.
- Moving tracked meeting notes outside the repository could lose content without byte-level verification or could mutate a broader external workspace unintentionally.
- Historical issue packages may retain links to deleted docs; updating all historical workflow artifacts would create new churn and is intentionally deferred.

### Canonical Docs

- `AGENTS.md` — point verifier terminology/current truth to the new canonical handbook.
- `CONTEXT-MAP.md` — update verifier documentation/glossary routing if referenced.
- `docs/verifiers-impl/README.md` — reduced navigation and reading order.
- `docs/verifiers-impl/current-state.md` — rewritten as the expansive canonical handbook with latest evidence.
- `docs/verifiers-impl/roadmap.md` — detailed future-only roadmap.
- `docs/verifiers-impl/decisions/` — curated active rationale and navigation.
- `mkdocs.verifier.yml` and `scripts/verifier-docs` — reconcile with the retained canonical docs if the site workflow remains useful.

## Delivery

| Outcome | Status | Proof |
|---|---|---|
| One canonical verifier handbook and deliberate reading order | complete | Current-state evidence values checked against package/preflight/execution/impact JSON; MkDocs build and stale-link search passed |
| Historical/reference documentation pruned and meeting notes safely relocated | complete | Live docs reduced to eight Markdown files; 15 external notes match their original Git blobs |
| Obsolete HTML/static-report output removed while v3 stays intact | complete | Focused 36-test Maven run passed; application spec asserts exactly five normal v3 artifacts plus path/source-boundary regressions |
| Final composition independently reviewed | complete | Fresh strong-tier SP reviewer converged from four `NEEDS_FIX` findings to `PASS` with no findings remaining |

## Decisions and Scope Deltas

| ID | Decision | Status | Reason |
|---|---|---|---|
| D1 | Use an expansive `current-state.md` as the sole current handbook, including latest evidence, reproduction commands, artifacts, interpretation, terms, and limitations | approved | User wants one solid answer source rather than many specialized pages |
| D2 | Keep only the latest representative evidence in current docs; replace rather than append historical baselines | approved | Prevent evidence chronology from recreating documentation bloat |
| D3 | Keep `README.md` as navigation and `roadmap.md` as a detailed future-only document | approved | Establish a predictable reading order and distinct ownership |
| D4 | Remove the in-repository archive; move meeting-note content to `/home/andre/thesis/meeting-notes/` and rely on Git for engineering history | approved | Meeting notes cross implementation and thesis writing; archived repo docs remain visible to agents and grep |
| D5 | Curate decisions by active non-obvious consequence, merging short rationale and deleting obsolete/superseded records without record-by-record user approval | approved | Prior sessions show real ADR value, but not enough to justify loose automatic retention |
| D6 | Fold useful reference/glossary material into the canonical story and update project instructions; SP itself requires no glossary file | approved | Terms and operation should be discoverable in reading order rather than a parallel hierarchy |
| D7 | Remove HTML report generation, renderer, configuration, tests, and stale unconsumed human-report logging; normal generation should expose exactly five v3 files | approved | The report predates the current package model, is large, and has no current downstream consumer |
| D8 | Do not restructure the broader thesis workspace or existing Overleaf clones in this package | approved | Only the meeting-note destination is needed for this coherent outcome |

## Acceptance Evidence

- `./scripts/verifier-docs build` passed with no missing-page/link warnings; the reduced site contains README, current state, roadmap, and four active decisions.
- `cd verifiers && mvn -Dtest=ScenarioGeneratorApplicationSpec,ApplicationAnalysisStateSpec,GroovyConstructorInputTraceVisitorSpec test` passed 36 tests with zero failures/errors/skips.
- `ScenarioGeneratorApplicationSpec` now asserts exactly the five v3 package files for normal enabled generation; no HTML/report path or formatter references remain under `verifiers/src`.
- Current-state headline values were checked directly against:
  - `verifiers/target/outcome2-helper-tracing/quizzes-20260727-180306-391/scenario-catalog-manifest.json` (`732` accepted/workloads, `82` candidates, `650` blocked, `164` FaultScenarios);
  - `verifiers/target/integral-numeric-restoration/setup-preflight-report.json` (`SUCCESS`, `82/82 SETUP_READY`);
  - targeted all-zero/single-fault v4 reports (`SUCCESS / EXACT`, `COMPENSATED / EXACT`);
  - the ImpactV1 sidecar (`EVALUATED`, count/score `0`).
- Fifteen meeting-note files were copied to `/home/andre/thesis/meeting-notes/` and compared byte-for-byte before deleting the repository archive; broader thesis workspace paths were not modified.
- `git diff --check` and current-tree stale-reference searches passed.

## Completion

Delivered:

- one canonical verifier handbook with inline terminology, operation, latest evidence, reproduction, interpretation, limitations, and safe claims;
- a future-only detailed roadmap and four curated active decision records;
- removal of the glossary/evidence/reference/advisor/guide/archive hierarchy and stale current narratives;
- byte-preserved relocation of 15 meeting notes to `/home/andre/thesis/meeting-notes/`;
- removal of the HTML renderer, automatic report output/configuration, full human-report formatter/logging, and report-only tests;
- exact five-file normal v3 generation coverage while preserving path confinement and `src/test/groovy` source boundaries;
- reconciled `AGENTS.md`, `CONTEXT-MAP.md`, Compose configuration, and MkDocs navigation.

Acceptance proof:

- focused Maven: 36 tests passed;
- verifier docs build: passed;
- current local links/stale-reference scan: passed;
- evidence claims: checked against current JSON artifacts;
- external meeting notes: all 15 match original `HEAD` blobs; no broader thesis workspace paths were changed;
- independent review: final `PASS`, no remaining findings;
- `git diff --check`: passed.

Limitations/deferrals:

- v3 schemas and metric fields were not changed;
- historical issue packages may retain links to removed historical docs;
- broader `/home/andre/thesis` organization and existing Overleaf clones were not changed;
- no commit, push, or PR was requested or performed.
