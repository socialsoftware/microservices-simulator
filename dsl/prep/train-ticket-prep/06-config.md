# 06 — nebula.config.json stub (Train-Ticket)

```json
{
  "projectName": "train-ticket",
  "basePackage": "com.trainticket",
  "consistencyModels": ["sagas"],
  "database": {
    "type": "mongodb",
    "host": "PLACEHOLDER",
    "port": 27017,
    "name": "train_ticket_db",
    "username": "PLACEHOLDER",
    "password": "PLACEHOLDER"
  },
  "java": {
    "version": "21",
    "springBootVersion": "3.5.3"
  }
}
```

## Non-trivial choices

- **`basePackage`** = `com.trainticket` — **PLACEHOLDER**. Source uses per-service packages like `order`, `travel`, `train`, `station` (no common com.* prefix). The Nebula tooling expects a single base package for the generated monolith-of-microservices. `com.trainticket` is a conventional choice; confirm with user.
- **`database.type`** = `mongodb` — the source uses **MongoDB** across nearly every service (Spring Data MongoDB with `@Document` annotations and `MongoRepository`). This is different from the `postgresql` default in the skill's example stub. Confirm whether Nebula's generator supports MongoDB; if not, we need to translate to Postgres (most entities are small and flat, so translation is feasible, but flags like `List<String>` fields on Route / Trip become join tables).
- **`consistencyModels`** = `["sagas"]` — Train-Ticket currently uses **no distributed-consistency mechanism** (naive REST with no compensation). Sagas is the closest fit for operations like "book a trip" (order + seat + payment + optional food/assurance/consign). TCC would also fit; **ask user** whether both should be generated for comparison (matching the `applications/quizzes-full` pattern).
- **`java.version`** — Train-Ticket sources target Java 8–11 (older). Nebula default 21 + Spring Boot 3.5.3 is fine for regeneration; we are not trying to preserve source runtime.

## Open questions for the user
1. MongoDB or Postgres for the generated target?
2. `sagas`, `tcc`, or both?
3. Base package name (`com.trainticket`, `org.trainticket`, `tt`, …)?
4. Project name — `train-ticket`, `trainticket`, `ts`?
