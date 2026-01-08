# Nebula DSL Developer Guide

**Version 1.0.0** · January 2026

---

## Table of Contents

| Step | Topic | Description |
|------|-------|-------------|
| [01](01-setup-and-cli.md) | Setup & CLI | Install toolchain, first generation |
| [02](02-entities-and-dtos.md) | Entities & Relationships | Root entities, supporting entities, JPA mappings |
| [03](03-dto-mappings.md) | DTO Schemas | `uses dto` syntax, cross-aggregate mappings |
| [04](04-repositories.md) | Custom Repositories | Repository block, JPQL queries |
| [05](05-services.md) | Service Layer | `@GenerateCrud`, Unit of Work, factories |
| [06](06-web-api.md) | Web API Endpoints | Controllers, functionalities, REST endpoints |
| [07](07-events.md) | Events & Subscriptions | Publishing, subscribing, event handlers |
| [08](08-sagas.md) | Saga Workflows | Distributed transactions, compensation |

---

## Quick Start

**New to Nebula?** Start with steps 01–06 for core microservice generation.

**Building distributed systems?** Continue with steps 07–08 for event-driven architecture and saga coordination.

---

## CLI Reference

```bash
./bin/cli.js generate <abstractions-path> [options]

Options:
  -o, --output <dir>    Output directory (default: ../../applications)
```

**Examples:**

```bash
# Generate from a folder of .nebula files
./bin/cli.js generate ../abstractions/answers/

# Generate to a custom output directory
./bin/cli.js generate ../abstractions/answers/ -o ./output
```

---

## Additional Resources

- [Full Language Reference](../../README.md) — DSL overview and examples
- [Grammar Definition](../../nebula/src/language/nebula.langium) — Langium grammar file
- [Working Examples](../../abstractions/answers/) — Real-world abstraction files

---

## Changelog

### v1.0.0 — January 2026

**Initial Release**

- **01-setup-and-cli**: Environment setup and CLI usage
- **02-entities-and-dtos**: Entity definitions and JPA relationship mapping
- **03-dto-mappings**: Cross-aggregate DTO schemas with `uses dto` syntax
- **04-repositories**: Custom repository queries and `@Query` annotations
- **05-services**: Service layer generation with `@GenerateCrud`
- **06-web-api**: REST controller and functionalities layer generation
- **07-events**: Event publishing, subscriptions, and handlers
- **08-sagas**: Saga workflow orchestration and compensation

---

## Contributing

To update this documentation:

1. Edit or create markdown files using the `NN-topic.md` pattern
2. Update the table of contents above
3. Add a changelog entry with the date and description
4. Maintain navigation links between pages
