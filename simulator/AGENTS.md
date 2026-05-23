# simulator/ — Core Library

The core transactional model library. Install to local Maven before building applications:

```bash
cd simulator
mvn install
```

---

## Exported Packages

| Package | Key Classes | Purpose |
|---------|------------|---------|
| `ms.domain.aggregate` | `Aggregate`, `AggregateDto`, `AggregateRepository`, `AggregateIdGeneratorService` | Base aggregate lifecycle |
| `ms.domain.event` | `Event`, `EventSubscription`, `EventHandler`, `EventApplicationService`, `EventService` | Domain event publication and subscription |
| `ms.domain.version` | `VersionService`, `IVersionService` | Global monotonic version counter |
| `ms.coordination.workflow` | `WorkflowFunctionality`, `Workflow`, `Step`, `FlowStep`, `AsyncStep` | Workflow DAG engine |
| `ms.coordination.workflow.command` | `Command`, `CommandGateway`, `CommandHandler`, `CommandResponse` | Command routing |
| `ms.coordination.unitOfWork` | `UnitOfWork`, `UnitOfWorkService` | Read/write coordination |
| `ms.sagas.aggregate` | `SagaAggregate`, `SagaState`, `GenericSagaState` | Semantic lock interface |
| `ms.sagas.workflow` | `SagaWorkflow`, `SagaStep`, `SagaAsyncStep` | Saga-specific workflow |
| `ms.sagas.unitOfWork` | `SagaUnitOfWork`, `SagaUnitOfWorkService` | Saga UoW lifecycle |
| `ms.causal.aggregate` | `CausalAggregate` | TCC merge interface |
| `ms.causal.workflow` | `CausalWorkflow` | TCC-specific workflow |
| `ms.causal.unitOfWork` | `CausalUnitOfWork`, `CausalUnitOfWorkService` | Causal UoW lifecycle |

## What to Extend

| Goal | Extend / implement |
|------|--------------------|
| New aggregate | Extend `Aggregate`; implement `verifyInvariants()` and `getEventSubscriptions()` |
| Saga support | Implement `SagaAggregate`; add `XxxSagaState` enum |
| TCC support | Implement `CausalAggregate`; add `getMutableFields()`, `getIntentions()`, `mergeFields()` |
| New workflow | Extend `WorkflowFunctionality`; build a `SagaWorkflow` or `CausalWorkflow` |
| New command | Extend `Command`; route in `CommandGateway` implementation |

## Concepts

- [Aggregate versioning](../docs/concepts/aggregate.md)
- [Sagas semantic locks](../docs/concepts/sagas.md)
- [TCC merge](../docs/concepts/tcc.md)
- [Events](../docs/concepts/events.md)
