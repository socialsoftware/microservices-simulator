# Step 1 · Environment & First Generation

This guide walks through Nebula DSL from a developer’s perspective. Each step adds another capability to the abstraction and points you to the next page when you are ready.

> **Goal of this step:** get the toolchain working and generate code from the simplest possible abstraction.

## Install & Build Once

Run everything from the repo root:

```bash
cd dsl/nebula
npm install
npm run langium:generate && npm run build
```

- `langium:generate` keeps the generated AST/types aligned with the grammar.
- `npm run build` transpiles the CLI, bundles the VS Code extension, and copies templates into `out/cli/`.

You only need to repeat these commands when you change the DSL or update dependencies.

## Project Layout & Naming

Keep abstractions inside `dsl/abstractions/<project>/`. One aggregate per `.nebula` file—create multiple files when your system needs multiple aggregates.

Example:

```
dsl/
└── abstractions/
    └── demo/
        ├── user.nebula
        └── car.nebula
```

## First CLI Run

From `dsl/nebula/`, run:

```bash
./bin/cli.js generate \
  --input ../abstractions/demo/user.nebula \
  --output ../../applications/demo \
  --architecture microservices
```

- `--input` points to the abstraction file.
- `--output` is where Java/Spring code lands.
- `--architecture` can be `microservices`, `causal-saga`, or `monolith`. Start with `microservices`.

After generation check `applications/demo/` for `microservices/user/aggregate/*`, `shared/dtos/*`, and the default service/repository layers.

Next we will enrich the abstraction with multiple entities, DTO mappings, and cross-aggregate DTO reuse.

➡️ [Next → Step 2 · Entities & DTOs](02-entities-and-dtos.md)

