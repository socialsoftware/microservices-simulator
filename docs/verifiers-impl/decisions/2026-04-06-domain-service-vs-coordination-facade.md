# Decision: distinguish domain services from coordination facades structurally

Date: 2026-04-06

## Status

Accepted.

## Context

A class annotated as a Spring `@Service` and holding a unit-of-work service is not necessarily a domain service. Coordination facades can have the same surface shape while creating unit-of-work objects and launching workflows.

Using package names, class-name suffixes, or naming conventions would make the verifier brittle and application-specific.

## Decision

Classify domain services through command-handler dispatch structure.

A domain service is a class that is a dispatch target of a command handler. A coordination facade is recognized by structural signals such as:

- creating unit-of-work objects;
- instantiating workflow/saga functionality classes;
- calling `executeWorkflow(...)`, `resumeWorkflow(...)`, or `executeUntilStep(...)`;
- orchestrating through a command gateway without being a command-handler dispatch target.

The verifier pipeline should prefer:

1. collect command-handler dispatch targets;
2. classify only dispatch-target service classes as domain services;
3. map command handlers to service methods and aggregate access policies.

## Rationale

- This matches application semantics instead of naming conventions.
- It prevents coordination facades from contaminating `state.services`.
- It keeps scenario footprints based on actual command dispatch and domain service effects.

## Consequences

- Dummyapp fixtures should include positive domain-service examples and negative coordination-facade examples.
- Visitor tests should assert semantic extraction results, not AST trivia.
- Ambiguous cases should be represented with minimal fixtures.

## Revisit when

- The application introduces framework patterns where command-handler dispatch targets are not statically visible.
- Profile-aware service resolution becomes necessary.
- Facades need their own explicit analysis model beyond creation-site extraction.
