# Step 3 · Where to Go Next

> **Goal of this step:** outline the remaining DSL capabilities so you know what to explore after mastering entities and DTOs.

[← Back to Step 2](02-entities-and-dtos.md)

## Upcoming Sections (WIP)

| Topic | What it unlocks | Status |
| --- | --- | --- |
| Repositories (`Repository { ... }`) | Custom queries, saga-safe filters, Spring Data integration | next |
| Services & Workflows | Generated service layer, orchestration DSL, saga workflows | planned |
| Events & Messaging | Publish/subscribe primitives, causal/saga extensions | planned |
| Web API Endpoints | DSL-driven controller generation & annotations | planned |

Each of these will get its own page in this folder, continuing the story from the same `demo` abstractions. The idea is to keep layering features on top of the existing aggregates so you can see how the code evolves.

## Contributing or Extending the Guide

- Add a new markdown file in `dsl/docs/developer-guide/` using the `NN-topic.md` pattern.
- Link it from the table above and from the previous/next pages.
- Keep the examples cumulative so a developer can follow along from step 1 without jumping around.

Until those sections are filled, feel free to peek at:

- `dsl/nebula/src/language/nebula.langium` for the full grammar.
- `dsl/nebula/src/cli/generators/**` to inspect how each construct is translated into Java.

More content coming soon!

