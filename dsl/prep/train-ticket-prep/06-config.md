# 06 — nebula.config.json (train-ticket, booking core)

A config **already exists** at `dsl/abstractions/trainTicket/nebula.config.json`. It is
consistent with the simulator's conventions (single base package, postgres) and should be
**reused** rather than replaced. Reproduced here for reference:

```json
{
    "projectName": "trainTicket",
    "basePackage": "pt.ulisboa.tecnico.socialsoftware",
    "consistencyModels": ["sagas"],
    "database": {
        "type": "postgresql",
        "host": "postgres",
        "port": 5432,
        "name": "trainticketdb",
        "username": "postgres",
        "password": "password"
    },
    "java": { "version": "21", "springBootVersion": "3.5.3" }
}
```

## Notes / divergences from source
- **basePackage** — source has **no common base package**; each service uses its own
  top-level package (`order`, `user`, `contacts`, `train`, `route`, `travel`, `price`,
  `fdse.microservice` for station). The simulator standardises on
  `pt.ulisboa.tecnico.socialsoftware` (same as quizzes/answers). Keep the simulator value.
- **database.type** — source uses **MySQL** (`jdbc:mysql://…`, `MySQL5Dialect`), some
  services use Mongo. The simulator/core library targets **PostgreSQL**; the existing config
  correctly overrides to `postgresql`. This is intentional — **not** a faithful port of the
  source DB. ✔ keep postgres.
- **consistencyModels** — set to `["sagas"]` only.
  - ⚠ **Question:** train-ticket's order-creation flow (`ts-preserve-service`) is a
    multi-step orchestration (security → contacts → seat → order → assurance/food) — a
    natural **Sagas** fit, which matches the current setting. Confirm whether any flow
    warrants **TCC** (e.g. seat reservation as a try/confirm/cancel). For booking-core as
    scoped, **`sagas` is sufficient**; add `tcc` only if seat hold/confirm is modelled
    explicitly later.
- **java / springBoot** — `21` / `3.5.3` match the simulator toolchain (source is older
  Spring Cloud; irrelevant to the port). ✔ keep.

**Recommendation:** no change needed — reuse the existing file.
