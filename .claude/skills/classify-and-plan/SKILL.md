---
name: classify-and-plan
description: Generate plan.md for Phase 1 (Classify & Plan). Parses domain-model.md and aggregate-grouping.md, classifies consistency rules using rule-enforcement-patterns, topologically sorts aggregates, and produces a comprehensive job queue. Invoke with /classify-and-plan <path/to/{App}-domain-model.md> <path/to/{App}-aggregate-grouping.md>
argument-hint: "<path/to/{App}-domain-model.md> <path/to/{App}-aggregate-grouping.md>"
---

# Phase 1: Classify & Plan

This skill automates Phase 1 of the microservices-simulator workflow. It reads domain and aggregation specifications, applies rule classification logic, and produces `plan.md` — a comprehensive, ready-to-execute job queue for Phase 2 agents.

The output (plan.md) is the job queue every downstream session starts from: it identifies which aggregates to implement in which order, which rules go where, and which test scenarios are needed. It is a blueprint, not a manifest - see the preamble emitted in Step 8.

## Input

The skill is invoked as:
```
/classify-and-plan <path/to/{App}-domain-model.md> <path/to/{App}-aggregate-grouping.md>
```

Example:
- `/classify-and-plan applications/{app-name}/{app-name}-domain-model.md applications/{app-name}/{app-name}-aggregate-grouping.md`

> **If arguments are missing or incorrect**, ask the user: "Please provide two file paths: domain-model.md and aggregate-grouping.md. Example: `/classify-and-plan path/to/domain-model.md path/to/aggregate-grouping.md`"

Both file paths must:
- Point to existing markdown files
- Resolve relative to the repository root (current working directory)
- Contain the required sections (§1–§4 for aggregates, §3.1–§3.2 and §4 for domain model)

**Extract the app name** from the domain-model.md filename: if the file is named `{AppName}-domain-model.md`, the app-name is `{AppName}`. The output `plan.md` will be written to `applications/{app-name}/plan.md`.

---

## Process

### Step 0: Anchor to the repository root

Before Step 1, read `.claude/skills/_shared/conventions.md` and follow "Anchor to the repository
root". Do not run any command until you have.

### Step 1: Validate Input Files

Before processing:

1. **Verify both files exist** — use absolute paths; if files don't exist, halt with a clear error: `"File not found: {path}. Please provide valid file paths."`

2. **Extract app-name** from the domain-model.md filename:
   - Pattern: `{AppName}-domain-model.md`
   - Example: `my-app-domain-model.md` → app-name = `my-app`
   - Error if pattern doesn't match: `"Domain model filename must match pattern '{AppName}-domain-model.md'. Got: {filename}"`

3. **Verify required sections exist** in domain-model.md (check for section headers):
   - Section heading: `## §3 — Rules` (contains §3.1 and §3.2 subsections)
   - Section heading: `### 3.1 — Single-entity rules` (table)
   - Section heading: `### 3.2 — Cross-entity rules` (blocks with headings)
   - Section heading: `## §4 — Functionalities` (table)
   - Halt if any are missing: `"Domain model is missing required section {section}. Please check the file structure."`

4. **Verify required sections exist** in aggregate-grouping.md:
   - Section heading: `## §1 — Aggregate Grouping` (table)
   - Section heading: `## §2 — Snapshots` (table)
   - Section heading: `## §3 — Upstream / Downstream Event Dependencies` (ASCII diagram)
   - Section heading: `## §4 — Events` (table)
   - Halt if any are missing: `"Aggregate grouping is missing required section {section}. Please check the file structure."`

5. **Verify `applications/{app-name}/harness-log.md` exists.** If it does not, halt:
   `"harness-log.md missing for {app-name}. Run /boot-strap first."` The file carries the run's
   self-healing mode in its header and only `/boot-strap` writes it, so its absence means Phase 0
   never ran for this application. Do not create it here and do not proceed without it: a plan built
   for an application that was never bootstrapped has nothing to be implemented into.

6. **Read the mode** from that header - `**Self-healing:** on|off`, missing or unrecognised meaning
   `off` (`.claude/skills/_shared/conventions.md` § "Harness log"). It governs this session's own
   Type 1 handling and is reported in Step 9.

---

### Step 2: Parse Domain-Model.md

Extract three distinct datasets from the domain-model.md:

#### 2.a: Parse §3.1 — Single-Entity Rules

Extract from the §3.1 table (columns: Rule, Entity, Predicate):
- Rule name (string)
- Entity name (string)
- Predicate (string)

**Output:** List of tuples `{rule_name, entity, predicate}`.

**Note:** All §3.1 rules are automatically classified as P1 (Intra-Invariant). No `rule-enforcement-patterns.md` consultation needed for these.

#### 2.b: Parse §3.2 — Cross-Entity Rules

Extract from the custom block format. Each rule is a separate block whose shape
`docs/templates/domain-model-template.md` § 3.2 defines, and that template is the authority on it:

```
#### Rule: {RULE_NAME}[ ({Qualifier})]

| Field | Value |
|---|---|
| Entities | {Entity1}, {Entity2} |
| Predicate | {predicate} |
```

Two things vary and both are legitimate. The heading may carry a **parenthetical qualifier** naming
the aggregate the rule is stated against - it disambiguates two rules that share a name across
aggregates, and it is part of the rule's identity, so carry it into the Rule Classification table
verbatim. And the `| Field | Value |` header plus its separator row sit **between** the heading and
the `| Entities |` row; a pattern that expects `| Entities |` on the line after the heading matches
nothing the template produces.

**Regex pattern to find rule blocks:**
```
#### Rule: ([A-Z_0-9]+)(?: \(([^)]*)\))?[^\n]*\n(?:[^\n]*\n)*?\| Entities \|([^|]*)\|[^\n]*\n\| Predicate \|(.*)\|
```

For each match:
- `rule_name` = captured group 1
- `qualifier` = captured group 2 (the aggregate the rule is stated against; absent for most rules)
- `entities` = captured group 3 (comma-separated aggregate/entity names)
- `predicate` = captured group 4 (the condition)

**Output:** List of tuples `{rule_name, qualifier, entities, predicate}`.

**Ambiguity handling:** If parsing fails for a rule block (malformed table or missing fields), flag as `"Needs review — Rule {rule_name} has unusual format"` and continue. Do not halt.

#### 2.c: Parse §4 — Functionalities

Extract from the §4 table (columns: Functionality, Primary Aggregate, Other Aggregates, Kind, Description):
- Functionality name (string)
- Kind (`Write` or `Read`; match case-insensitively)
- Primary Aggregate (string)
- Other Aggregates (comma-separated or "—" for none)
- Description (string)

**Operation type is read from the `Kind` column, never inferred.** The domain model declares it; the
description is prose and is not evidence. Do not apply keyword heuristics to the description and do
not prompt the user to disambiguate — a missing or unrecognised `Kind` value is a malformed spec:
halt with `"Functionality '{functionality_name}' has no valid Kind (expected 'Write' or 'Read'). Fix §4 of the domain model."`

**Output:** List of tuples `{functionality_name, operation_type, primary_aggregate, other_aggregates, description}`.

---

### Step 3: Parse Aggregate-Grouping.md

Extract three distinct datasets:

#### 3.a: Parse §1 — Aggregates

Extract from the §1 table (columns: Aggregate, Description, Entities contained, Service):
- Aggregate name (string)
- Description (string)
- Entities contained (string, may be comma-separated)
- Service class name (string)

**Output:** List of tuples `{aggregate_name, description, entities, service}`. Build a map `aggregate_name → {description, entities, service}` for later lookup.

#### 3.b: Parse §3 — Dependency DAG

Extract from the ASCII diagram. The diagram shows aggregate dependencies via arrows.

**Pattern:** Lines containing arrows like:
- `Source ──► Target` (long dash)
- `Source → Target` (short arrow)
- `Source --> Target` (ASCII arrow)

**Regex pattern:**
```
^\s*([A-Za-z0-9_]+)\s+(?:──►|→|-->)\s+([A-Za-z0-9_]+)
```

For each match:
- `source_aggregate` = captured group 1
- `target_aggregate` = captured group 2
- Interpret as: "source publishes events to target" or "target subscribes to events from source"

**Output:** List of edge tuples `{publisher, subscriber}`. Build a graph structure (adjacency list) `publisher → [subscribers]`.

**Ambiguity handling:** If the DAG is malformed or missing aggregates mentioned in §1, flag as `"Needs review — DAG has unmapped aggregates"` and continue using available edges.

#### 3.c: Parse §4 — Events

Extract from the §4 table (columns: Event, Publisher, Trigger, Payload fields, Consumer(s)):
- Event name (string)
- Publisher (aggregate name)
- Trigger (description)
- Payload fields (comma-separated or "—" for none)
- Consumers (comma-separated aggregate names)

**Output:** List of event tuples `{event_name, publisher, trigger, payload_fields, consumers}`. Build maps:
- `event_name → {publisher, trigger, payload_fields, consumers}`
- `(publisher, consumer) pair → [event_names]` for quick lookup

#### 3.d: Parse §2 — Snapshots

From the §2 table (columns: Aggregate, Snapshots of, Fields cached, Updated on event):
- `aggregate_name` = the aggregate that caches the snapshot
- `snapshots_of` = the source entity name being cached (string, may contain `× N`)
- `updated_on_event` = the events that refresh the cached copy, or `n/a` / `—` for none

**Collection snapshots** — `× N` appears on the row, in **either** the "Aggregate" column or the
"Snapshots of" column. Both forms are in use: `| Warehouse | Shipment × N | ... |` marks the
collection on the source, while `| Warehouse / WarehouseSlot × N | Shipment | ... |` marks it on the
owned entity that holds it. Scan both cells — a row whose `× N` sits in the "Aggregate" column is a
collection snapshot exactly like any other, and reading only the "Snapshots of" cell silently
misclassifies it as a single snapshot and drops its `Dto`.

Collection snapshots are stored as a `@OneToMany` set of owned entities, and **always** get their own
entity class plus a `{OwnedEntity}Dto.java`. A set needs an element type; there is nothing to collapse
onto the aggregate.

**Single snapshots** — no `× N`. Whether these get an owned-entity class depends on one thing only:

> A single snapshot needs an `aggregate/{OwnedEntity}.java` class **iff it subscribes to events** —
> its "Updated on event" cell names at least one event. The subscription class is constructed from the
> cached snapshot, which must supply the `(subscribedAggregateId, subscribedVersion)` pair, and the
> harness standardises that carrier as an owned entity, so that every subscription — single or
> collection — is constructed from a reference object of the same shape, and so that the
> service-layer ByEvent discriminator has a stable object to match the incoming event against.
>
> This is a uniformity rule, not a framework constraint. `EventSubscription`'s constructor takes
> `(Integer subscribedAggregateId, Long subscribedVersion, String eventType)` — plain scalars — so a
> subscription *could* be built from fields held directly on the aggregate. Do not "simplify" it that
> way: the two snapshot kinds would then need two different subscription shapes for no gain.
>
> A single snapshot whose "Updated on event" cell is `n/a` (or empty) subscribes to nothing, so no
> subscription is ever constructed for it. It is cached **directly on the aggregate** as an id field,
> exactly as §1 of the domain model describes single references. **Emit no class for it.**
>
> **No version field on an `n/a` row.** A cached version exists for exactly one purpose: to build the
> `(subscribedAggregateId, subscribedVersion, eventType)` triple an `EventSubscription` needs. A row
> that never subscribes never constructs one, so the version would be written, persisted and never
> read. Add a version field **iff** the row's "Updated on event" cell names at least one event. An
> application with no events therefore caches no versions at all.

Single snapshots never need a separate Dto in either case.

**Output:** For each aggregate, three lists:
- `collection_snapshot_entities[agg]` → owned entity class names, one per `× N` row; each needs both
  an entity class and a `{OwnedEntity}Dto.java`
- `subscribing_single_snapshot_entities[agg]` → owned entity class names for single snapshots with a
  non-empty "Updated on event"; each needs an entity class, no Dto
- `inline_single_snapshots[agg]` → single snapshots with no "Updated on event"; **no files at all**,
  they become fields on the aggregate

---

### Step 4: Classify §3.2 Rules Using Decision-Guide

Apply the flowchart in `docs/concepts/rule-enforcement-patterns.md` § Decision Guide to each §3.2
rule, in the order that doc specifies (same-aggregate → P4a/P4b saga-structural guarantees →
synchronous P3 → eventual P2). That section is authoritative for what each pattern means and when
it applies — do not re-derive the classification logic here.

**Deferred rules.** A §3.2 block marked *(deferred)* carries prose instead of an Entities/Predicate
table. This is not a parse failure and must not be flagged "needs review" — the domain model is
stating that the rule is out of scope for this run. Record it in the Rule Classification table with
pattern `— (deferred)` and an implementation note of the form:

```
Deferred by the domain model — do not implement. No pattern is assigned and no session produces
code for it. Revisit only if a future revision of §3.2 gives it an Entities/Predicate table.
```

Deferred rules are excluded from Step 6.c cross-aggregate prerequisites and from every session's
file list.

**Parser heuristic** (mechanical only — this keyword matching exists to drive unattended parsing;
it is not part of the doc's decision criteria).

The two P4 branches read a proxy each, so the block is runnable without a human in the loop:

- `rule_is_implicitly_enforced_by_fetch(rule)` — the rule's predicate names a lookup that §4 of the
  domain model already describes the saga performing, keyed so the command throws when the
  precondition is unmet (typically a compound-key `Get…By…And…Command`).
- `rule_holds_by_shared_value_in_same_saga(rule)` — the rule equates a field across two aggregates
  that a single §4 saga creates or updates in the same run, passing that value to both.

Both are parse proxies, not the criteria. A rule either proxy catches is still classified by
`docs/concepts/rule-enforcement-patterns.md` § Decision Guide, and where proxy and guide disagree the
guide wins.

```
FOR each rule in §3.2:
  IF rule block is marked (deferred):
    classification = "— (deferred)"
    CONTINUE

  entities = parse(rule.entities)
  predicate = rule.predicate

  IF all entities in predicate refer to same aggregate (check aggregates map):
    classification = P1
  ELSE IF rule_is_implicitly_enforced_by_fetch(rule):
    classification = "P4a"
  ELSE IF rule_holds_by_shared_value_in_same_saga(rule):
    classification = "P4b"
  ELSE IF predicate contains sync keywords ("immediately", "synchronous", "before", "prevents", "blocks", "forbids"):
    classification = P3
  ELSE IF predicate contains eventual-consistency keywords ("eventually", "async", "eventually consistent") OR rule is about caching:
    classification = P2
  ELSE:
    classification = "P3 (NEEDS_REVIEW)"  # ambiguous: mark for review

  rules_classified[rule.name] = {
    pattern: classification,
    entities: entities,
    predicate: predicate
  }
```

The Rule Classification table's "Implementation note" column (Step 8) uses the pattern's
authoritative description from `docs/concepts/rule-enforcement-patterns.md` § Quick Reference,
substituting this rule's specific aggregate/event/field names. For `P3 (NEEDS_REVIEW)`, write
`P3 — needs review` and flag for manual resolution.

---

### Step 5: Build Dependency Graph and Topologically Sort Aggregates

Using edges from §3 (DAG):

1. **Build adjacency list:**
   ```
   graph[publisher] = [subscribers...]
   reverse_graph[subscriber] = [publishers...]
   ```

2. **For each aggregate, determine upstream dependencies:**
   ```
   upstream_deps[agg] = aggregates_whose_events_agg_subscribes_to
                      = [pub for pub in reverse_graph[agg]]
   ```

3. **Topological sort** using Kahn's algorithm:
   ```
   in_degree = {agg: len(reverse_graph[agg]) for agg in aggregates}
   queue = [agg for agg in aggregates if in_degree[agg] == 0]  # no deps
   sorted_aggregates = []
   
   WHILE queue not empty:
     agg = queue.pop(0)
     sorted_aggregates.append(agg)
     FOR subscriber in graph[agg]:
       in_degree[subscriber] -= 1
       IF in_degree[subscriber] == 0:
         queue.append(subscriber)
   
   IF len(sorted_aggregates) != len(aggregates):
     ERROR: "Circular dependency detected in DAG"
   ```

**Output:** Ordered list of aggregates `[agg1, agg2, ..., aggN]` such that each aggregate comes after all its publishers.

---

### Step 5.5: Detect Reverse Dependencies (Deferred Guards and Deferred Reads)

After the topological sort, check for **two** kinds of dependency that run backwards against it. The
topological sort is driven by P2 event-subscription edges only; neither kind below is a DAG edge, so
neither affects ordering, but both create a runtime dependency that must be tracked explicitly.

**5.5a — Reverse P3 guards.** P3 DTO-check rules where the required data comes from an aggregate ordered _later_ than the aggregate that owns the guard.

```
FOR each aggregate A at position i in sorted_aggregates:
  FOR each P3 rule R that requires a saga data-assembly fetch from aggregate B
      (i.e. R is classified P3 DTO-check variant and R.data_source_aggregate == B):
    IF position(B) > position(A):
      // B is ordered after A, but A's service guard reads B's DTO
      // This is a reverse P3 dependency — cannot be implemented in session 2.i.b
      Annotate R in A's cross-aggregate prerequisites as:
        "⚠️ DEFERRED — requires {B}Dto; implement in session 2.{j}.c after {B} is done"
      Add a note to B's aggregate section in plan.md:
        "After completing 2.{j}.c, revisit {A} session 2.{i}.c to add the deferred {R.name} guard"
```

**5.5b — Reverse reads.** A **read** functionality is not a rule and has no pattern, so 5.5a cannot
see it. Apply the same test to §4 directly: any read functionality whose "Other Aggregates" cell names
an aggregate ordered later than its own Primary Aggregate is unimplementable in its own session `b`.

```
FOR each read functionality F in §4:
  A = F.primary_aggregate
  FOR each B in F.other_aggregates:
    IF position(B) > position(A):
      Annotate F in A's read-functionality list as:
        "⚠️ DEFERRED — reads {B}; implement in a revisit session after 2.{j}.c"
      Add a note to B's aggregate section in plan.md:
        "After completing 2.{j}.c, revisit {A} session 2.{i}.b to implement {F}"
```

A deferred read does **not** hold up the rest of session `b`: every other read for that aggregate is
implemented normally and the session's checkbox is ticked. Only the marked functionality waits.

**Why this matters:** Without this step, the dependency is silently invisible in plan.md and the
implementing agent discovers the gap mid-session with no guidance — sending a command to a service
that does not exist yet. Surfacing it as a ⚠️ DEFERRED marker lets a session-`c` agent apply the
deferred-guard protocol from `session-c.md` immediately, and tells a session-`b` agent to skip the
marked read rather than improvise one.

**Output:** For each detected reverse dependency, plan.md must show:
- In aggregate `A`'s cross-aggregate prerequisites (5.5a) or read-functionality list (5.5b): the ⚠️ DEFERRED marker with a pointer to the unblocking session.
- In aggregate `B`'s section: a "revisit" note naming the session that must come back.

---

### Step 6: Map Functionalities to Aggregates and Identify Review Sessions

For each aggregate in sorted order:

#### 6.a: Extract write and read functionalities

```
write_functionalities = [f for f in all_functionalities 
                        if f.primary_aggregate == agg AND f.operation_type == write]
read_functionalities = [f for f in all_functionalities 
                       if f.primary_aggregate == agg AND f.operation_type == read]
```

#### 6.b: Extract published and subscribed events

From §4 events table:
```
published_events = [e for e in all_events if e.publisher == agg]
subscribed_events = [e for e in all_events if agg in e.consumers]
```

#### 6.c: Identify cross-aggregate data-assembly requirements for this aggregate

For each write functionality, identify rules classified as P3 (DTO field check variant) or P4a (construction prerequisite) that require a saga data-assembly step:
```
cross_agg_rules = [r for r in rules_classified 
                   if r.pattern in ('P3', 'P4a') AND 
                      r.requires_saga_fetch AND
                      any(entity in r.entities for entity in agg.entities)]
```

Map each rule to the saga data-assembly step that provides the needed data and, for P3 DTO-check rules, to the service method that performs the explicit validation.

#### 6.d: Compute the saga-state set for this aggregate

The `{Aggregate}SagaState` enum is emitted by session `a`, but the set of constants it must hold is
**decided here**, not there. Session `a` writes the domain layer before any saga exists, so deriving
the set at that point forces an agent to infer the shape of write sagas that other sessions - and
often other aggregates - have not written yet. A wrong inference surfaces only in a much later
session's `c`, as a missing constant or an unreferenced one.

Each **non-create write functionality** of this aggregate contributes exactly one constant, named
`IN_` + the functionality name in `SCREAMING_SNAKE_CASE` (`UpdateTopic` → `IN_UPDATE_TOPIC`,
`AddParticipant` → `IN_ADD_PARTICIPANT`). That is the state its saga's **primary lock step** acquires
via `setSemanticLock` — see `docs/concepts/sagas.md` § Lock-Acquisition Step Pattern and
§ Step Ordering step 3.

Nothing else contributes:

- **Create functionalities contribute no constant.** The aggregate does not exist when the saga
  starts, so there is no prior state to transition from (`sagas.md` § Create Functionality Sagas).
  An aggregate whose only write functionality is a create therefore has an **empty** enum, which is
  correct and must still be emitted — later aggregates may add write functionalities to it.
- **Being fetched by another aggregate contributes no constant.** A foreign saga's data-assembly step
  is a plain read, and a foreign saga's *mutating* step guards with
  `setForbiddenStates([{This}SagaState.IN_{OP}])` — it consumes the constants above rather than
  needing one of its own (`sagas.md` § R4 Decision Table).
- **`NOT_IN_SAGA` is never declared.** It is `GenericSagaState.NOT_IN_SAGA`, supplied by the
  framework.

```
saga_states[agg] = ['IN_' + screaming_snake(f.name)
                    for f in write_functionalities[agg]
                    if f.operation_type != create]
```

For each constant, record a one-line origin: the saga that acquires it, plus any **foreign** saga
that guards on it, which Step 6.c's cross-aggregate prerequisites already identify. The origin is
what lets a session `a` agent transcribe the line without re-deriving it, and what lets a reviewer
catch a constant that nothing acquires.

**Output:** for each aggregate, an ordered list of `(constant, origin)` pairs — emitted by Step 8 as
the `**Saga states:**` line of the aggregate section.

#### 6.e: Compute the domain sentinel set for this aggregate

A **domain sentinel** is a fixed literal that a write functionality assigns to a field, and that some
P1 predicate - in this aggregate or in a downstream one - compares against. It lives in the shared
`{src}microservices/domain/{AppClass}DomainConstants.java`, is emitted by session `a` of the
aggregate that **writes** it, and like the saga-state set it is **decided here**, not there.

The reason is the same as 6.d's, one step stronger. The session that writes the literal cannot see
the rule that gives it meaning: the writing aggregate is upstream, so its session `a` runs long
before the downstream aggregate's P1 predicate exists. Left to infer, that session emits an inline
string, and the downstream session finds no constant to import - so it inlines a second copy. Two
copies of a sentinel that later diverge do not fail a test; the predicate simply stops matching, and
every suite stays green.

**Detection.** Scan every predicate classified P1 in Step 4 (plus every §3.1 predicate, which is P1
by construction) for a comparison against a **literal** - a quoted string, or a bare numeric or
boolean constant that is not another field of the same predicate. Ordered-domain bounds are not
sentinels: a threshold in `count <= 5` is a rule parameter, not a value any write functionality
assigns. The test is whether some §4 functionality *writes* it: a bound appearing only inside a
comparison (`remainingCapacity <= 5`) is a rule parameter the domain model states; a literal some §4
Description says an operation *sets* (`ReleaseShipment` sets `carrierName` to `"unassigned"`) is a
sentinel.

**Attribution.** The sentinel belongs to the aggregate whose **write functionality assigns it**, read
from the §4 Description column - not to the aggregate whose rule compares against it. Attribution to
the writer is what makes the ordering safe: a sentinel is compared against a snapshot field cached
from the writer, so the writer is always upstream in the Step 5 topological order and always has the
lower ordinal. Its session `a` therefore always precedes the consuming aggregate's.

**Naming.** `SCREAMING_SNAKE_CASE` of the literal's domain meaning. Where the literal is already a
word, the constant matches it.

For each sentinel, record the writing functionality plus every rule and aggregate that compares
against it. That origin is what lets session `a` transcribe the line without re-deriving it, and what
lets a reviewer catch a constant nothing writes or nothing reads.

```
sentinels[agg] = [(name(lit), lit, origin)
                  for r in rules_classified if r.pattern == P1
                  for lit in literals(r.predicate)
                  if writer_of(lit) == agg]
```

**Output:** for each aggregate, an ordered list of `(constant, literal, origin)` triples - emitted by
Step 8 as the `**Domain sentinels:**` line of the aggregate section, on both the writing aggregate
(which declares it) and every consuming aggregate (which imports it). Worked example, `Shipment`
writes it and `Carrier` compares against it:

```
- `UNASSIGNED = "unassigned"` - written by `ReleaseShipment`; compared by `CARRIER_IS_ASSIGNED` (`Carrier`)
```

---

### Step 7: Generate File Lists for Each Session (2.N.a–d)

For each aggregate, generate the full file list using the templates below (this skill is the
authoritative source for the file-list shape; `docs/workflow.md` only points here):

Paths in the tables below resolve against **three** roots, and the leading segment says which:

- `commands/...`, `events/...`, and any path written with an explicit `{src}` prefix are rooted at the
  **app source root**, `{src}`. Commands and events are shared across services and deliberately do
  not live inside a microservice package - see `docs/workflow.md` § "Package layout" and
  `docs/concepts/commands.md` § "File Location".
- `sagas/...` paths are Groovy tests, rooted at the **app test root**, `{tgt-test}` as defined in
  `.claude/skills/_shared/conventions.md` § "Resolve aggregate context".
- Everything else is relative to the aggregate's own package, `{src}microservices/{aggregate}/`.

**Session 2.N.a — Domain Layer:**
```
| Session | Files |
|---------|-------|
| 2.N.a | `aggregate/{Aggregate}.java`, `aggregate/{OwnedEntity}.java` (per §1 entity), `aggregate/{DomainEnum}.java` (per enum-typed §1 attribute), `aggregate/{CollectionSnapshotEntity}.java` (per × N snapshot from §2), `aggregate/{CollectionSnapshotEntity}Dto.java` (per × N snapshot entity), `aggregate/{SubscribingSnapshotEntity}.java` (per single §2 snapshot that subscribes to an event - see below), `aggregate/{Aggregate}Factory.java`, `aggregate/{Aggregate}CustomRepository.java`, `aggregate/sagas/Saga{Aggregate}.java`, `aggregate/sagas/states/{Aggregate}SagaState.java`, `aggregate/sagas/factories/Sagas{Aggregate}Factory.java`, `aggregate/sagas/repositories/{Aggregate}CustomRepositorySagas.java`, `aggregate/{Aggregate}Dto.java`, `aggregate/{Aggregate}Repository.java`, `{Aggregate}ServiceApplication.java`, `{src}microservices/exception/{AppClass}ErrorMessage.java` (edited), `{AppClass}SpockTest.groovy` (edited), `sagas/{aggregate}/{Aggregate}IntraInvariantTest.groovy`, `{src}microservices/domain/{AppClass}DomainConstants.java` (only if Step 6.e gave this aggregate a sentinel to declare) |
```

> **`(edited)` entries** are files that already exist and are appended to, not created. Session `a`
> appends one error-message constant per P1 rule and a `create{Aggregate}(...)` fixture helper; session
> `b` appends the functionalities field to the same test base class. They are listed so the row does not
> need amending on every aggregate.

> **`{AppClass}DomainConstants.java` is conditional and shared.** List it in the 2.N.a row of every
> aggregate whose Step 6.e sentinel list is non-empty, and only those - a *consuming* aggregate
> imports the class but produces nothing in it. Like the `{src}commands/{aggregate}/` entries and
> `{src}ServiceMapping.java`, it is rooted at the app source root rather than at
> `microservices/{aggregate}/`, because it is shared across microservices. The first aggregate that
> declares a sentinel creates it; every later one appends.

> **Never omit from 2.N.a:** `{Aggregate}Factory.java`, `{Aggregate}CustomRepository.java` and
> `{Aggregate}ServiceApplication.java` must always appear in the 2.N.a row — the factory and
> repository even when the aggregate has no cross-table lookups, and the service application
> unconditionally, since it is the per-aggregate Spring entry point rather than a domain artifact.
> Every owned entity class listed in the §1 "Entities contained" column must appear individually.
>
> **Domain enums are derivable from §1 and must be listed.** Scan the §1 attribute types of the
> aggregate and every owned entity: any attribute whose type is not a primitive, a `String`, a date/time
> type, an id reference or another listed entity is a domain enum and needs its own
> `aggregate/{DomainEnum}.java`. Name the file after the type as written in §1. Enumerating them here
> is what stops session `a` from discovering an unresolvable type mid-file.
>
> **An enum named by more than one aggregate is emitted once, at the app source root.** Scan §1 as a
> whole before assigning enum paths. A type that only one aggregate's attributes name stays at
> `aggregate/{DomainEnum}.java` in that aggregate's own package. A type that **two or more**
> aggregates name is one domain concept, so emit `{src}enums/{DomainEnum}.java` and list that path in
> the 2.N.a row of **every** aggregate that names it, suffixed `(shared)`. The earliest such session
> creates the file; the later ones import it unchanged. Copying it into each aggregate package instead
> would turn one concept into several mutually unassignable Java types, forcing a name-based
> conversion wherever a value crosses an aggregate boundary and letting a value added to one copy
> drift from the others.
>
> **Which §2 snapshots get a class** — apply the rule from Step 3.d:
> - every `× N` collection snapshot → one `aggregate/{CollectionSnapshotEntity}.java` **and** one
>   `aggregate/{CollectionSnapshotEntity}Dto.java`;
> - a single snapshot **with** a non-empty "Updated on event" → one
>   `aggregate/{SubscribingSnapshotEntity}.java`, no Dto;
> - a single snapshot whose "Updated on event" is `n/a` → **no file**; it is cached as an id field and
>   a version field on the aggregate itself.
>
> **⚠️ Collection-snapshot entity classes are commonly missed.** After filling in the 2.N.a file cell,
> do a final pass: for every `× N` row in §2 for this aggregate, verify its entity class appears as a
> separate entry. If it is absent, add it now — the entity class is required for the aggregate to
> compile even before any service code is written.

**Session 2.N.b — Read Functionalities:**
```
| Session | Files |
|---------|-------|
| 2.N.b | `service/{Aggregate}Service.java` (read methods), `messaging/{Aggregate}CommandHandler.java`, `{src}commands/{aggregate}/Get{Aggregate}ByIdCommand.java`, `{src}commands/{aggregate}/{Query}Command.java` (one per read op), `coordination/sagas/{Query}FunctionalitySagas.java` (one per read op), `coordination/functionalities/{Aggregate}Functionalities.java`, `{src}ServiceMapping.java` (add the `{AGGREGATE}` entry), `{AppClass}SpockTest.groovy` (edited), `sagas/{aggregate}/{Aggregate}ServiceTest.groovy` (read-method cases), `sagas/coordination/{aggregate}/{Query}Test.groovy` (one per read op) |
```

> **`Get{Aggregate}ByIdCommand.java` is unconditional** — list it in every aggregate's 2.N.b row, whether or not §4 has any read functionality for that aggregate. Write sagas need it for their get-then-lock step, so it is infrastructure rather than a domain read, and session `b` is therefore never empty.

> **`{Aggregate}Functionalities.java`:** Always include this file — it is required as a Spring bean for test wiring regardless of whether read functionalities exist.

> **`{src}ServiceMapping.java` is unconditional and shared.** Every aggregate needs an entry, because
> every command constructor resolves its target through `ServiceMapping.{AGGREGATE}.getServiceName()`.
> Like the `{src}commands/{aggregate}/` entries, it is rooted at the app source root rather than at
> `microservices/{aggregate}/`, and it is edited, not created, for every aggregate after the first.
> It belongs to session `b` because that is where the aggregate's first command is written.

**Session 2.N.c — Write Functionalities:**
```
| Session | Files |
|---------|-------|
| 2.N.c | `service/{Aggregate}Service.java` (write methods appended), `{src}commands/{aggregate}/{Operation}Command.java` (one per write op), `coordination/sagas/{Operation}FunctionalitySagas.java` (one per write op), write coordinator methods appended to `coordination/functionalities/{Aggregate}Functionalities.java`, write cases appended to `messaging/{Aggregate}CommandHandler.java`, `coordination/webapi/{Aggregate}Controller.java`, `sagas/coordination/{aggregate}/{Operation}Test.groovy` (one per write op), `sagas/coordination/{aggregate}/{Operation}CompensationTest.groovy` and `applications/{app-name}/src/test/resources/groovy/{Operation}CompensationTest/{Operation}FunctionalitySagas.csv` (per write op whose saga holds a semantic lock across a later step), write-method cases plus event-publication assertions appended to `sagas/{aggregate}/{Aggregate}ServiceTest.groovy` |
```

> **The compensation-test pair is listed for every write functionality, gated by a note.** Whether a
> given operation needs it is decided in session `c` by the applicability test in
> `.claude/skills/implement-aggregate/session-c.md` § "One `{Op}CompensationTest.groovy` per
> lock-holding write functionality", which owns that rule — do not restate or re-derive it here. List
> both files with the gate wording above so a session that does need them never has to amend plan.md,
> and the impairment CSV is never forgotten: the test injects no fault without it and fails for an
> unrelated reason. Which functionalities need the pair is decided in session `c` by the applicability
> test named above.

> **`{Aggregate}Controller.java` is unconditional** — a minimal `@RestController` stub under
> `coordination/webapi/`. List it for every aggregate; it is not gated on the aggregate having any
> HTTP-facing functionality.

> **Event classes:** If Events published is non-empty, append one `events/{Event}Event.java` per
> published event to the session-c file list — the class keeps the `Event` suffix exactly as §4 names
> it. These are produced in session c alongside the service methods that publish them.

**Session 2.N.d — Event Wiring** (omit if no subscribed events):
```
| Session | Files |
|---------|-------|
| 2.N.d | `notification/subscribe/{Aggregate}Subscribes{Event}.java` (one per subscribed event), `notification/handling/{Aggregate}EventHandling.java`, `notification/handling/handlers/{Aggregate}EventHandler.java`, `coordination/eventProcessing/{Aggregate}EventProcessing.java`, `coordination/functionalities/{Aggregate}Functionalities.java` (one `{operation}ByEvent` appended per event), `service/{Aggregate}Service.java` (one mutate helper appended per event), `aggregate/{Aggregate}.java` (`getEventSubscriptions()` updated), `sagas/{aggregate}/{Aggregate}InterInvariantTest.groovy` |
```

**Substitution rules:**
- `{Aggregate}` → aggregate name (PascalCase, e.g., "Warehouse")
- `{AGGREGATE}` → aggregate name in SCREAMING_SNAKE_CASE, as it appears in `ServiceMapping` (e.g., "WAREHOUSE")
- `{OwnedEntity}` → the §1 "Entities contained" name **verbatim**. Do not prepend the aggregate name:
  a §1 entry of `Shipment` yields `Shipment.java`, not `WarehouseShipment.java`. Prepend the aggregate
  name **only** to break an actual collision — another aggregate in §1 already claims that class name,
  or the name is already taken by an aggregate class. Prepending by reflex produces names that stutter
  when the §1 entity is already qualified.
- `{DomainEnum}` → an enum-typed attribute's type name from §1, verbatim (e.g., "ShipmentStatus").
  Rooted at `aggregate/` when one aggregate names it, at `{src}enums/` when several do - see the
  2.N.a note above
- `{CollectionSnapshotEntity}` → owned entity class name for each `× N` snapshot in §2. Which name
  depends on which of the two row forms §2 uses:
  - **`| {Aggregate} | {Source} × N | ... |`** — the row names only the source, so derive the class
    name by qualifying it with the owning aggregate. Snapshot entities are the standing exception to
    the verbatim rule here, because the bare source name collides with the source aggregate's own
    class: a `Shipment × N` snapshot cached by `Warehouse` becomes `WarehouseShipment`.
  - **`| {Aggregate} / {OwnedEntity} × N | {Source} | ... |`** — the row already names the owned
    entity. Use that name **verbatim**; do not qualify it again. A `| Warehouse / WarehouseSlot × N |
    Shipment |` row yields `WarehouseSlot`, never `WarehouseShipment` or `WarehouseWarehouseSlot`.

  Cross-check the result against the §1 "Entities contained" column of the aggregate grouping, which
  is authoritative: the class name you emit must appear there. Appears twice in 2.N.a, once for the
  entity class and once for the Dto; omit if no `× N` rows exist for this aggregate.
- `{SubscribingSnapshotEntity}` → same naming as above, for each single §2 snapshot with a non-empty
  "Updated on event"; omit if this aggregate has none
- `{Operation}` → write operation name (PascalCase, e.g., "AddShipment")
- `{Query}` → read operation name (PascalCase, e.g., "GetOpenShipments")
- `{Event}` → event name **without** the "Event" suffix (e.g., "UpdateWarehouseName" for
  "UpdateWarehouseNameEvent"). It is used bare only inside
  `notification/subscribe/{Aggregate}Subscribes{Event}.java`; the event class itself is
  `events/{Event}Event.java`, which restores the suffix.
- `{aggregate}` → aggregate name (kebab-case or lowercase, e.g., "warehouse")
- `{src}` → the app source root, `applications/{app-name}/src/main/java/pt/ulisboa/tecnico/socialsoftware/{pkg}/`
- `{App}` → app name (PascalCase, e.g., "MyApp")

---

### Step 8: Construct plan.md

Write the output file to: `applications/{app-name}/plan.md`

Structure the file as follows:

#### Header and Introduction
```markdown
# {AppName} Implementation Plan

Generated by Phase 1. Every session agent reads this file first and ticks its checkbox last.

> **This plan is a blueprint, not a manifest.** A file absent from a Files-to-produce table may
> still be required - the concept docs and session sub-files are the authority on what an aggregate
> needs. Amending this file mid-implementation is expected. Mark any added row with the session that
> added it and a one-line reason.

---
```

The blockquote is emitted verbatim, immediately under the "Generated by Phase 1" line.
(Substitute `{AppName}` derived from domain-model.md filename.)

#### Rule Classification Table
```markdown
## Rule Classification

All §3.2 rules from {App}-domain-model.md classified by docs/concepts/rule-enforcement-patterns.md.

| Rule name | Pattern | Implementation note |
|-----------|---------|---------------------|
```

Rows: one per §3.2 rule
- Column 1: rule name (as extracted)
- Column 2: pattern (P1, P2, P3, P4a, P4b, "P3 (NEEDS_REVIEW)" if ambiguous, or `— (deferred)` for a
  rule the domain model marks *(deferred)*)
- Column 3: implementation note (from Step 4 classification); for a deferred rule, the
  do-not-implement note from Step 4

Note: Include §3.1 rules as a separate subsection if desired, all marked as P1.

#### Aggregate Implementation Order Table
```markdown
## Aggregate Implementation Order

Topological sort of the dependency DAG (§3 of aggregate-grouping.md).

| # | Aggregate | Upstream deps | Events published | Events subscribed | Sessions |
|---|-----------|--------------|-----------------|-------------------|---------|
```

Rows: one per aggregate (in sorted order from Step 5)
- Column 1: # (ordinal: 1, 2, 3, ...)
- Column 2: aggregate name
- Column 3: upstream deps (comma-separated, or "—" for none)
- Column 4: events published (comma-separated event names, or "—" for none)
- Column 5: events subscribed (comma-separated event names, or "—" for none)
- Column 6: sessions (space-separated: `a b c` or `a b c d` depending on presence of write/read/subscribed events)

#### Aggregate Details Sections

For each aggregate in sorted order. **The heading must be `### {N}. {Aggregate}`, where `{N}` is the
aggregate's ordinal from the Implementation Order table** — `_shared/conventions.md`
§ "Resolve aggregate context" locates the section by that exact shape and halts if it is absent, so
every Phase 2 skill depends on the ordinal being present.

```markdown
### {N}. {Aggregate}

**Write functionalities** (mutating operations from §4 of domain-model.md):
- `{Operation}({args})` — description

**Read functionalities** (query operations):
- `{Query}({args})` — description

**Events published:** list from aggregate-grouping §4
**Events subscribed:** list from aggregate-grouping §4

**Cross-aggregate prerequisites** (P4a rules and P3 DTO-check rules requiring a saga data-assembly fetch):
- `{RuleName}` → `{Operation}FunctionalitySagas` data-assembly step (fetch from `{OtherAggregate}`)

**Saga states** (`{Aggregate}SagaState` — from Step 6.d; session `a` transcribes this list verbatim):
- `IN_{OPERATION}` — acquired by `{Operation}FunctionalitySagas` primary lock step
- `IN_{OPERATION}` — acquired by `{Operation}FunctionalitySagas` primary lock step; guarded by `{OtherOperation}FunctionalitySagas` (`{OtherAggregate}`) via `setForbiddenStates`

**Domain sentinels** (`{AppClass}DomainConstants` - from Step 6.e; session `a` transcribes this list verbatim):
- `{CONSTANT} = "{literal}"` - written by `{Operation}`; compared by `{RULE_NAME}` (`{OtherAggregate}`)

**Files to produce:**

| Session | Files |
|---------|-------|
| 2.N.a | `aggregate/{Aggregate}.java`, ... |
| 2.N.b | `service/{Aggregate}Service.java` (read methods), ... |
| 2.N.c | `service/{Aggregate}Service.java` (write methods appended), ... |
| 2.N.d | `notification/subscribe/{Aggregate}Subscribes{Event}.java`, ... |

**Checklist:**
- [ ] 2.N.a — Domain layer
- [ ] 2.N.b — Read functionalities
- [ ] 2.N.c — Write functionalities
- [ ] 2.N.d — Event wiring

---
```

(Omit Session 2.N.d section if Events subscribed is empty.)

The **Saga states** line is never omitted. When Step 6.d yields no constant, emit it as
`**Saga states:** none — {reason}` (typically: the aggregate's only write functionality is a create).
An absent line is indistinguishable from a forgotten one, and session `a` halts on it rather than
guessing.

The **Domain sentinels** line is never omitted either, for the same reason, and has two `none` forms
that must not be collapsed into one:

- `**Domain sentinels:** none.` - the aggregate neither declares nor compares against one.
- `**Domain sentinels:** none declared. `{RULE_NAME}` compares against `{CONSTANT}`, declared by `{Aggregate}`.`
  - the aggregate is a *consumer*. Its session `a` produces no constant but must import the class
  rather than re-derive the literal, so the line has to say so.

The checklist above is the shape **before** slices are emitted. Step 8.5 expands it.

#### The harness log already exists

This skill does **not** create `applications/{app-name}/harness-log.md`. `/boot-strap` does, and it
is the sole declarer of the run's self-healing mode in that file's header. Creating it here would
mean creating it without a declared mode, and a missing header reads as OFF, so a run could end up in
a mode nobody chose (Step 1 halts before this point when the file is absent).

Any friction this session encountered with the harness (a doc or skill that failed to guide the
parsing or classification) is appended as a row with `Session` = `1`, per
`conventions.md` § "Harness log". The Type 1 / Type 2 gates in `AGENTS.md` § "Harness evolution"
apply to this session as they do to every other, in the mode the header declares.

---

### Step 8.5: Emit the Slice List for Each Session

A Phase 2 session can be implemented by one agent or split across several, one per unit of work. The
split is decided **here**, at plan time, and written into plan.md as sub-checkboxes under the session
checkbox. It is not a runtime judgement: emitting it here makes it a reviewable, reproducible run
artifact, identical on every re-run of the same spec.

`/implement-aggregate-full` reads this list and spawns one subagent per slice.
`/implement-aggregate` ignores it and implements the whole session. Both entry points require it to
be present.

#### The threshold

| Session | Slices |
|---------|--------|
| `a` | never sliced - no sub-checkboxes |
| `b` | never sliced - no sub-checkboxes |
| `c` | one per write functionality **if the count is > 3**; otherwise no sub-checkboxes |
| `d` | one per subscribed event **if the count is > 3**; otherwise no sub-checkboxes |

Session `a` produces a single aggregate and is indivisible. Session `b` is small by construction.
Sessions `c` and `d` are the only ones whose size varies with the domain, and each slice re-reads the
session's concept docs, so paying that cost for a two-item session is waste.

A session at or below the threshold gets **no** sub-checkboxes; it is one implicit slice covering the
whole session. This is not a second control path - a session always has a slice list, and below the
threshold it has exactly one entry, which the manager runs the same way it runs any other.

#### Slice ids and ordering

Slice id is `2.{N}.{type}{k}`, with `k` starting at `1`, numbered in **execution order**. Execution
order is not arbitrary: the manager runs slices sequentially in the emitted order, and later slices
depend on earlier ones.

**Session `c` ordering:**

1. The **create** write functionality is always slice 1. Session `c` rewires the
   `create{Aggregate}()` helper in `{AppClass}SpockTest.groovy` to go through the real create saga,
   and every later slice's test `setup:` depends on that helper.
2. Then any write functionality that operates on state another write functionality produces, after
   the one that produces it.
3. Otherwise, §4 order of the domain model.

**Session `d` ordering:** subscribed events carry no inter-slice dependency. Use the §4 event order
of the aggregate-grouping spec.

#### Emitted shape

Sub-checkboxes are nested one level under their session checkbox. The item name is the write
functionality name (session `c`) or the event name as §4 spells it (session `d`).

```markdown
**Checklist:**
- [ ] 2.N.a — Domain layer
- [ ] 2.N.b — Read functionalities
- [ ] 2.N.c — Write functionalities
  - [ ] 2.N.c1 {Operation1}
  - [ ] 2.N.c2 {Operation2}
  - [ ] 2.N.c3 {Operation3}
  - [ ] 2.N.c4 {Operation4}
- [ ] 2.N.d — Event wiring
  - [ ] 2.N.d1 {Event1}
  - [ ] 2.N.d2 {Event2}
  - [ ] 2.N.d3 {Event3}
  - [ ] 2.N.d4 {Event4}
```

An aggregate with three write functionalities and two subscribed events emits neither `c` nor `d`
sub-checkboxes, and its checklist is exactly the four-line form from Step 8.

Report the slice counts in the Step 9 summary: how many sessions were sliced, and into how many
slices in total.

---

### Step 9: Report Success

After writing plan.md:

1. **Confirm completion:**
   ```
   ✓ Phase 1 plan generated successfully.
   Plan written to: applications/{app-name}/plan.md
   Self-healing: {mode, from the harness-log.md header}
   ```

2. **Summary of results:**
   - Total aggregates processed: N
   - Total rules classified: M (broken down by pattern: P1: X, P2: Y, P3: Z, P4a/b: R)
   - Ambiguous rules flagged for review: K (marked "P3 (NEEDS_REVIEW)")
   - Deferred rules recorded but not implemented: D
   - Total Phase 2 sessions: count (e.g., "2.1.a through 2.3.d")
   - Sessions sliced (Step 8.5): S, into T slices in total

3. **Next steps:**
   ```
   Next: Phase 2 — aggregate-by-aggregate implementation.
   Read plan.md and start with session 2.1.a (first aggregate, domain layer).
   ```

---

## Critical Implementation Notes

### Parsing Edge Cases

1. **§3.2 rule blocks with unusual formatting:**
   - If table format varies (e.g., "| Entities |" vs "| Entity |"), use best-effort regex matching
   - Flag result as "Needs review — unusual format detected"
   - Continue processing

2. **ASCII DAG with many arrow styles:**
   - Accept: `──►`, `→`, `-->`, `==>`
   - Fallback: if none found, try line-by-line heuristic (look for words "publishes to", "sends to", "subscribes")

3. **Circular dependencies in DAG:**
   - Halt with error: "Circular dependency detected between aggregates: {list}. Please check aggregate-grouping.md §3."
   - Do NOT attempt topological sort

4. **Functionalities with a missing or unrecognised `Kind`:**
   - Halt; do not guess and do not prompt. §4 declares write-vs-read and is the only source for it
   - Report the offending functionality name so the domain model can be corrected

### Ambiguity Flagging

Mark sections that need human review:

- **"Needs review — Rule {name} has unusual format":** Parsing issue; content may be incomplete
- **"P3 — needs review":** Classification ambiguity between P3 (explicit service guard) and P4a (implicit in saga fetch); user to decide
- **"Needs review — DAG has unmapped aggregates":** Aggregate in rules but not in DAG
- **"Needs review — unusual format detected":** Parsing used fallback heuristic

The user can review these flags before Phase 2 begins.

### File Path Conventions

- Input: Both paths are relative to repository root; convert to absolute if necessary
- Output: `applications/{app-name}/plan.md` is relative to repository root
- All file references in plan.md use forward slashes and are relative to `applications/{app-name}/src/`

---

## Notes

- The skill does not run tests or validate the plan against code — that is Phase 2's responsibility.
- The skill does not create any source files - `plan.md` only. `harness-log.md` is created by
  `/boot-strap`; this skill reads its header and appends rows, and never writes the header.
- Phase 2 agents will read plan.md and tick checkboxes as they complete each session.
- If plan.md already exists, overwrite it with the newly generated version (this allows re-planning if the domain model changes).
- For ambiguous sections, users can manually edit plan.md before Phase 2 begins; Phase 2 agents will read the current version.
