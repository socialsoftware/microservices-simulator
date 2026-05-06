# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Build Commands

```bash
cd nebula

# First-time setup
npm install

# Full build (grammar → TypeScript → bundle → copy templates)
npm run langium:generate && npm run build

# Generate Java code from .nebula abstractions
./bin/cli.js generate ../abstractions/answers/
./bin/cli.js generate ../abstractions/answers/ -o ./output -d -v

# Lint
npm run lint
```

The `build` script runs: `tsc -b tsconfig.src.json && node esbuild.mjs && cp -r src/cli/templates out/cli/`. Templates are plain Handlebars files and must be copied separately since they are not TypeScript.

After editing the grammar (`nebula.langium`), always run `npm run langium:generate` before `npm run build` — Langium auto-generates the AST types in `src/language/generated/`.

---

## Architecture

Nebula is a **Langium-based DSL** that transforms `.nebula` aggregate definitions into Spring Boot Java microservices for the [Microservices Simulator](../README.md).

### Generation Pipeline

```
.nebula files → Langium Parser → AST → Validators → Generator Registry → Feature Facades → Handlebars Templates → Java files
```

Six phases in `CodeGenerator.generateCode()` (`src/cli/engine/code-generator.ts`):

1. **Discovery** — scan directory for `.nebula` files
2. **Parsing** — Langium builds AST, resolves cross-references, registers models globally
3. **Validation** — structural + semantic checks
4. **Configuration** — load `nebula.config.json`, initialize generator registry
5. **Generation** — iterate aggregates through feature facades
6. **Completion** — write project-level files (pom.xml, application.yml, entry point)

### Key Layers

| Layer | Location | Role |
|-------|----------|------|
| Grammar | `src/language/nebula.langium` | DSL syntax (~500 lines), defines all constructs: Aggregate, Entity, Method, Workflow, Events, References, etc. |
| Validators | `src/language/validation/` | Semantic checks (entity, property, invariant, repository, method, naming) |
| Engine | `src/cli/engine/` | Orchestration: `code-generator.ts` (pipeline), `generator-registry.ts` (22 generators with dependency tracking), `project-setup.ts` |
| Feature facades | `src/cli/features/` | Coordinate multiple generators per domain concern: Entity, Service, Events, Coordination, WebApi, Saga, Validation |
| Generators | `src/cli/generators/` | Organized by layer: `base/`, `microservices/`, `coordination/`, `sagas/`, `validation/`, `common/` |
| Templates | `src/cli/templates/` | Handlebars templates organized by domain: entity, repository, service, events, saga, web, config, plus `_partials/` |
| Utilities | `src/cli/utils/` | Type resolution (`unified-type-resolver.ts`), file writing, naming helpers |
| Services | `src/cli/services/` | DTO schema registry for cross-generator coordination |

### Design Patterns

- **Registry** — `GeneratorRegistryFactory` registers all 22 generators with metadata, versioning, and dependency tracking
- **Facade** — Each feature facade (e.g., `EntityFeature`, `SagaFeature`) coordinates multiple generators for a single concern
- **Template Method** — `GeneratorBase` provides shared helpers (type resolution, naming, template rendering) to all generators
- **Builder** — `EventContextBuilder`, `DtoSetterBuilder` for complex object construction

### Inline vs Template Generation

Most generators render Handlebars templates via `GeneratorBase.renderTemplate()`. However, `CommandGenerator`, `CommandHandlerGenerator`, and `ServiceMappingGenerator` (v3.0) use **inline string concatenation** because their output depends heavily on runtime analysis and is too dynamic for templates.

### Cross-File Type Resolution

A global model registry (`aggregate-helpers.ts`) enables aggregates in different `.nebula` files to reference each other. `UnifiedTypeResolver` (`src/cli/utils/unified-type-resolver.ts`) maps DSL types to Java types with context awareness (entity vs DTO vs WebApi vs Service contexts).

---

## Abstractions

`.nebula` files in `abstractions/` define domain models. Each project folder contains:
- One `.nebula` file per aggregate
- `shared-enums.nebula` for shared enum types
- `exceptions.nebula` for custom exception messages (optional)
- `nebula.config.json` for project settings (package name, database, Java version, consistency model)

Current case studies: `answers/` (11 aggregates, saga-based) and `teastore/` (6 aggregates).

---

## VSCode Extension

Pre-built at `extensions/nebula-extension-0.0.2.vsix`. Provides syntax highlighting, autocomplete, and real-time validation for `.nebula` files via LSP. Extension source is in `nebula/src/extension/`.

To rebuild: `npm run vscode:prepublish` then `npm run release`.
