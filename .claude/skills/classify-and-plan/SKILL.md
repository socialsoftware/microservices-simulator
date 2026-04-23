---
name: classify-and-plan
description: Generate plan.md for Phase 1 (Classify & Plan). Parses domain-model.md and aggregate-grouping.md, classifies consistency rules using rule-enforcement-patterns, topologically sorts aggregates, and produces a comprehensive job queue. Invoke with /classify-and-plan <path/to/{App}-domain-model.md> <path/to/{App}-aggregate-grouping.md>
argument-hint: "<path/to/{App}-domain-model.md> <path/to/{App}-aggregate-grouping.md>"
---

# Phase 1: Classify & Plan

This skill automates Phase 1 of the microservices-simulator workflow. It reads domain and aggregation specifications, applies rule classification logic, and produces `plan.md` — a comprehensive, ready-to-execute job queue for Phase 2 and Phase 3 agents.

The output (plan.md) is the single source of truth for all downstream work: it identifies which aggregates to implement in which order, which rules go where, and which test scenarios are needed.

## Input

The skill is invoked as:
```
/classify-and-plan <path/to/{App}-domain-model.md> <path/to/{App}-aggregate-grouping.md>
```

Examples:
- `/classify-and-plan applications/quizzes-full/quizzes-full-domain-model.md applications/quizzes-full/quizzes-full-aggregate-grouping.md`
- `/classify-and-plan docs/examples/example-domain-model.md docs/examples/example-aggregate-grouping.md`

> **If arguments are missing or incorrect**, ask the user: "Please provide two file paths: domain-model.md and aggregate-grouping.md. Example: `/classify-and-plan path/to/domain-model.md path/to/aggregate-grouping.md`"

Both file paths must:
- Point to existing markdown files
- Resolve relative to the repository root (current working directory)
- Contain the required sections (§1–§4 for aggregates, §3.1–§3.2 and §4 for domain model)

**Extract the app name** from the domain-model.md filename: if the file is named `{AppName}-domain-model.md`, the app-name is `{AppName}`. The output `plan.md` will be written to `applications/{app-name}/plan.md`.

---

## Process

### Step 1: Validate Input Files

Before processing:

1. **Verify both files exist** — use absolute paths; if files don't exist, halt with a clear error: `"File not found: {path}. Please provide valid file paths."`

2. **Extract app-name** from the domain-model.md filename:
   - Pattern: `{AppName}-domain-model.md`
   - Example: `quizzes-full-domain-model.md` → app-name = `quizzes-full`
   - Error if pattern doesn't match: `"Domain model filename must match pattern '{AppName}-domain-model.md'. Got: {filename}"`

3. **Verify required sections exist** in domain-model.md (check for section headers):
   - Section heading: `## §3 — Consistency Rules` (contains §3.1 and §3.2 subsections)
   - Section heading: `### §3.1 — Single-Entity Rules` (table)
   - Section heading: `### §3.2 — Cross-Entity Rules` (blocks with headings)
   - Section heading: `## §4 — Functionalities` (table)
   - Halt if any are missing: `"Domain model is missing required section {section}. Please check the file structure."`

4. **Verify required sections exist** in aggregate-grouping.md:
   - Section heading: `## §1 — Aggregates` (table)
   - Section heading: `## §3 — Dependency DAG` (ASCII diagram)
   - Section heading: `## §4 — Events` (table)
   - Halt if any are missing: `"Aggregate grouping is missing required section {section}. Please check the file structure."`

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

Extract from the custom block format. Each rule is a separate block delimited by heading `#### Rule: {RuleName}`, followed by a table with rows for "Entities" and "Predicate".

**Regex pattern to find rule blocks:**
```
#### Rule: ([A-Z_0-9]+)\n.*?\n\| Entities \| ([^\|]+) \|\n\| Predicate \| ([^\|]+) \|
```

For each match:
- `rule_name` = captured group 1
- `entities` = captured group 2 (comma-separated aggregate/entity names)
- `predicate` = captured group 3 (the condition)

**Output:** List of tuples `{rule_name, entities, predicate}`.

**Ambiguity handling:** If parsing fails for a rule block (malformed table or missing fields), flag as `"Needs review — Rule {rule_name} has unusual format"` and continue. Do not halt.

#### 2.c: Parse §4 — Functionalities

Extract from the §4 table (columns: Functionality, Primary Aggregate, Other Aggregates, Description):
- Functionality name (string)
- Primary Aggregate (string)
- Other Aggregates (comma-separated or "—" for none)
- Description (string)

**Output:** List of tuples `{functionality_name, primary_aggregate, other_aggregates, description}`.

**Classify operation type** (write vs. read):
- **Write** if description contains: "create", "delete", "update", "add", "remove", "execute", "perform" (case-insensitive)
- **Read** if description contains: "get", "list", "find", "retrieve", "count", "query" (case-insensitive)
- **Ambiguous** if neither heuristic matches or both match with contradictory signals

**Ambiguity handling:** If operation type is ambiguous, prompt the user:
```
"The functionality '{functionality_name}' is unclear:
  Description: '{description}'
  Is this a (W)rite or (R)ead operation? (W/R): "
```
Wait for user input (single character). Default to Write if user input is invalid.

**Output:** List of tuples `{functionality_name, primary_aggregate, other_aggregates, description, operation_type}`.

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

---

### Step 4: Classify §3.2 Rules Using Decision-Guide

Apply the flowchart from `docs/concepts/rule-enforcement-patterns.md` to each §3.2 rule.

The flowchart asks:

1. **"Data all in same aggregate (with cached fields)?"**
   - If YES: → **P1** (intra-invariant, implement in `verifyInvariants()`)
   - If NO: → continue to question 2

2. **"Must be synchronous?"**
   - If YES: → **P3** (service guard)
     - If check needs data from another aggregate: add a data-assembly saga step to fetch it as a DTO, then validate in the service.
     - If the precondition is implicit in the fetch query (query fails when unmet): → **Construction prerequisite** (no explicit guard code needed).
   - If NO: → continue to question 3

3. (From NO branch) **"Eventually consistent acceptable?"**
   - If YES: → **P2** (inter-invariant, event subscription + polling)
   - If NO: → Escalate to **P3** (reconsider synchronism)

**Implementation algorithm:**

```
FOR each rule in §3.2:
  entities = parse(rule.entities)
  predicate = rule.predicate
  
  # Question 1: Same aggregate?
  IF all entities in predicate refer to same aggregate (check aggregates map):
    classification = P1  # intra-invariant (may use cached snapshot fields)
  ELSE:
    # Question 2: Saga-structural guarantees?
    # Is the precondition implicit in the fetch query?
    # (i.e., the fetch only succeeds when the precondition is met)
    IF rule_is_implicitly_enforced_by_fetch(rule):
      classification = "P4a"
    # Does the invariant hold because the same value is passed to two aggregates
    # in the same saga (no fetch needed, holds by construction)?
    ELSE IF rule_holds_by_shared_value_in_same_saga(rule):
      classification = "P4b"
    ELSE:
      # Question 3: Must be synchronous?
      # Keywords: "immediately", "synchronous", "before", "prevents", "blocks", "forbids"
      # P3 covers ALL synchronous service-layer checks — both own-table uniqueness reads
      # and DTO field validation from saga-assembled data. P4 covers ALL by-construction guarantees — P4a (prerequisite), P4b (construction invariant).
      IF predicate contains sync keywords:
        classification = P3  # service guard (*Service.java — own-table read OR DTO field check)
      ELSE:
        # Question 4: Eventually consistent?
        # Keywords: "eventually", "async", "eventually consistent"
        IF predicate contains eventual-consistency keywords OR rule is about caching:
          classification = P2
        ELSE:
          # Ambiguous: mark for review
          classification = "P3 (NEEDS_REVIEW)"
        
  rules_classified[rule.name] = {
    pattern: classification,
    entities: entities,
    predicate: predicate
  }
```

**Implementation notes** (to appear in Rule Classification table):

- **P1:** `intra-invariant in verifyInvariants()`
- **P3:** `service guard in {PrimaryAggregate}Service` — own-table uniqueness check, or DTO field validation from preceding saga step (both enforced in the service layer, never in saga code)
- **P2:** `inter-invariant — {Consumer}Aggregate subscribes to {Event}`
- **P4a:** `implicit in saga data-assembly — {Operation}FunctionalitySagas fetches {OtherAggregate}; query fails if precondition unmet`
- **P4b:** `implicit in saga construction — same {field} passed to both {AggA} and {AggB} in the same saga; holds by construction, no separate check needed`
- **P3 (NEEDS_REVIEW):** `P3 — needs review` (mark for manual resolution)

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

### Step 6: Map Functionalities to Aggregates and Identify Phase 3 Scenarios

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

#### 6.d: Phase 3 scenario identification (for aggregates with 2+ write functionalities)

**T4 — Concurrent-operation conflicts:**
- If 2+ write functionalities exist for this aggregate, they could race
- Create one T4 row per pair of conflicting write ops
- Example: "AddParticipant and UpdateTournament both modify Tournament → Session 3.N: AddParticipant + UpdateTournament | T4"

**T5 — Fault and Recovery scenarios:**
- For each write functionality: if its saga has 3+ steps, create T5 Fault and T5 Recovery rows
- Example: "CreateTournament has 5 saga steps → Session 3.N: CreateTournament Fault | T5; Session 3.N+1: CreateTournament Recovery | T5"

**T6 — Async stress:**
- If an aggregate has 2+ write functionalities with high concurrency risk, optionally add T6
- For now: add one T6 row per aggregate with 2+ write ops (user can remove if not needed)

---

### Step 7: Generate File Lists for Each Session (2.N.a–d)

For each aggregate, generate the full file list using the template from `docs/workflow.md` (lines 163–167):

**Session 2.N.a — Domain Layer:**
```
| Session | Files |
|---------|-------|
| 2.N.a | `aggregate/{Aggregate}.java`, `aggregate/{OwnedEntity}.java` (per entity), `aggregate/sagas/Saga{Aggregate}.java`, `aggregate/sagas/states/{Aggregate}SagaState.java`, `aggregate/sagas/factories/Sagas{Aggregate}Factory.java`, `aggregate/sagas/repositories/{Aggregate}CustomRepositorySagas.java`, `aggregate/{Aggregate}Dto.java`, `aggregate/{Aggregate}Repository.java`, `sagas/{aggregate}/{Aggregate}Test.groovy` |
```

**Session 2.N.b — Write Functionalities:**
```
| Session | Files |
|---------|-------|
| 2.N.b | `service/{Aggregate}Service.java` (write methods), `messaging/{Aggregate}CommandHandler.java`, `commands/{aggregate}/{Operation}Command.java` (one per write op), `coordination/sagas/{Operation}FunctionalitySagas.java` (one per write op), `sagas/coordination/{aggregate}/{Operation}Test.groovy` (one per write op) |
```

**Session 2.N.c — Read Functionalities:**
```
| Session | Files |
|---------|-------|
| 2.N.c | `service/{Aggregate}Service.java` (read methods appended), `commands/{aggregate}/Get{Query}Command.java` (one per read op), `coordination/sagas/{Query}FunctionalitySagas.java` (one per read op), `sagas/coordination/{aggregate}/{Query}Test.groovy` (one per read op) |
```

**Session 2.N.d — Event Wiring** (omit if no subscribed events):
```
| Session | Files |
|---------|-------|
| 2.N.d | `notification/subscribe/{Aggregate}Subscribes{Event}.java` (one per subscribed event), `notification/handling/{Aggregate}EventHandling.java`, `notification/handling/handlers/{Aggregate}EventHandler.java`, `coordination/eventProcessing/{Aggregate}EventProcessing.java`, `sagas/{aggregate}/{Aggregate}InterInvariantTest.groovy` |
```

**Substitution rules:**
- `{Aggregate}` → aggregate name (PascalCase, e.g., "Tournament")
- `{OwnedEntity}` → entity name per owned entity in aggregate
- `{Operation}` → write operation name (PascalCase, e.g., "AddParticipant")
- `{Query}` → read operation name (PascalCase, e.g., "GetOpenTournaments")
- `{Event}` → event name without "Event" suffix (e.g., "UpdateUserName" for "UpdateUserNameEvent")
- `{aggregate}` → aggregate name (kebab-case or lowercase, e.g., "tournament")
- `{App}` → app name (PascalCase, e.g., "QuizzesFull")

---

### Step 8: Construct plan.md

Write the output file to: `applications/{app-name}/plan.md`

Structure the file as follows:

#### Header and Introduction
```markdown
# {AppName} Implementation Plan

Generated by Phase 1. Every session agent reads this file first and ticks its checkbox last.

---
```
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
- Column 2: pattern (P1, P2, P3, P4a, P4b, or "P3 (NEEDS_REVIEW)" if ambiguous)
- Column 3: implementation note (from Step 4 classification)

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
For each aggregate in sorted order:

```markdown
### {Aggregate}

**Write functionalities** (mutating operations from §4 of domain-model.md):
- `{Operation}({args})` — description

**Read functionalities** (query operations):
- `Get{Query}({args})` — description

**Events published:** list from aggregate-grouping §4
**Events subscribed:** list from aggregate-grouping §4

**Cross-aggregate prerequisites** (P4a rules and P3 DTO-check rules requiring a saga data-assembly fetch):
- `{RuleName}` → `{Operation}FunctionalitySagas` data-assembly step (fetch from `{OtherAggregate}`)

**Files to produce:**

| Session | Files |
|---------|-------|
| 2.N.a | `aggregate/{Aggregate}.java`, ... |
| 2.N.b | `service/{Aggregate}Service.java`, ... |
| 2.N.c | `service/{Aggregate}Service.java` (read methods appended), ... |
| 2.N.d | `notification/subscribe/{Aggregate}Subscribes{Event}.java`, ... |

**Checklist:**
- [ ] 2.N.a — Domain layer
- [ ] 2.N.b — Write functionalities
- [ ] 2.N.c — Read functionalities
- [ ] 2.N.d — Event wiring

---
```

(Omit Session 2.N.d section if Events subscribed is empty.)

#### Phase 3 Session List
```markdown
## Phase 3 Session List

One row per aggregate that has concurrent-op conflicts or compensatable sagas.

| Session | Primary aggregate | Operations / scenarios | T4/T5/T6 |
|---------|------------------|------------------------|----------|
```

Rows: one per T4/T5/T6 scenario (from Step 6.d)
- Column 1: session number (3.1, 3.2, 3.3, ..., incremented per row)
- Column 2: primary aggregate
- Column 3: operations / scenarios (e.g., "AddParticipant + UpdateTournament", "CreateTournament Fault")
- Column 4: test type (T4, T5, or T6)

Followed by checklist:
```markdown
**Checklist:**
- [ ] 3.1
- [ ] 3.2
- [ ] 3.3
...
```

---

### Step 9: Report Success

After writing plan.md:

1. **Confirm completion:**
   ```
   ✓ Phase 1 plan generated successfully.
   Plan written to: applications/{app-name}/plan.md
   ```

2. **Summary of results:**
   - Total aggregates processed: N
   - Total rules classified: M (broken down by pattern: P1: X, P2: Y, P3: Z, P4a/b/c: R)
   - Ambiguous rules flagged for review: K (marked "P3 (NEEDS_REVIEW)")
   - Total Phase 2 sessions: count (e.g., "2.1.a through 2.3.d")
   - Total Phase 3 scenarios: count (e.g., "3.1 through 3.7")

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

4. **Functionalities with unclear operation type:**
   - Always prompt user; do not guess
   - Offer clear choices: "(W)rite / (R)ead"

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
- The skill does not create any source files — plan.md only.
- Phase 2 agents will read plan.md and tick checkboxes as they complete each session.
- If plan.md already exists, overwrite it with the newly generated version (this allows re-planning if the domain model changes).
- For ambiguous sections, users can manually edit plan.md before Phase 2 begins; Phase 2 agents will read the current version.
