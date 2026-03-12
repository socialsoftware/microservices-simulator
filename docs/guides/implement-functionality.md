# Guide: Implement a Functionality

A functionality is a cross-service operation built as a `WorkflowFunctionality` subclass. It orchestrates steps that may span multiple aggregates via the `CommandGateway`.

Reference: [`docs/concepts/sagas.md`](../concepts/sagas.md), [`docs/concepts/tcc.md`](../concepts/tcc.md)

---

## Step 1 — Create the command class

File: `<app>/command/<service>/XxxCommand.java`

```java
public class XxxCommand extends Command {
    private final Integer targetAggregateId;
    // payload fields

    public XxxCommand(UnitOfWork unitOfWork, String serviceId, Integer targetAggregateId, ...) {
        super(unitOfWork, serviceId);
        this.targetAggregateId = targetAggregateId;
    }
}
```

Commands that read data (no mutation) use a `GetXxxCommand` and return a DTO from the command handler.

---

## Step 2 — Implement: Sagas functionality

File: `microservices/<aggregate>/coordination/sagas/XxxFunctionalitySagas.java`

```java
public class XxxFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private SomeDto someDto;  // shared state between steps

    public XxxFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                  Integer arg1, Integer arg2,
                                  SagaUnitOfWork unitOfWork,
                                  CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(arg1, arg2, unitOfWork);
    }

    public void buildWorkflow(Integer arg1, Integer arg2, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep step1 = new SagaStep("step1", () -> {
            GetSomeDataCommand cmd = new GetSomeDataCommand(unitOfWork, ServiceMapping.SOME.getServiceName(), arg1);
            this.someDto = (SomeDto) commandGateway.send(cmd);
        });

        SagaStep step2 = new SagaStep("step2", () -> {
            // declare forbidden states to acquire semantic lock
            List<SagaAggregate.SagaState> forbidden = new ArrayList<>();
            forbidden.add(TargetSagaState.IN_CONFLICTING_OPERATION);

            XxxCommand cmd = new XxxCommand(unitOfWork, ServiceMapping.TARGET.getServiceName(), arg2, this.someDto);
            cmd.setForbiddenStates(forbidden);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(step1)));  // depends on step1

        this.workflow.addStep(step1);
        this.workflow.addStep(step2);
    }
}
```

Key patterns:
- Each step is a `SagaStep(name, lambda)` or `SagaStep(name, lambda, dependencies)`
- Shared data between steps is stored as fields on the functionality class
- Forbidden states are set via `cmd.setForbiddenStates(list)` on the consuming step

---

## Step 3 — Implement: TCC functionality

File: `microservices/<aggregate>/coordination/causal/XxxFunctionalityTCC.java`

```java
public class XxxFunctionalityTCC extends WorkflowFunctionality {
    public XxxFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                Integer arg1, Integer arg2,
                                CausalUnitOfWork unitOfWork,
                                CommandGateway commandGateway) {
        this.buildWorkflow(arg1, arg2, unitOfWork, unitOfWorkService, commandGateway);
    }

    public void buildWorkflow(...) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        // Steps follow the same pattern as Sagas but use CausalWorkflow
        // No SagaState — conflicts are resolved by merge, not locks
        FlowStep step1 = new FlowStep("step1", () -> { ... });
        this.workflow.addStep(step1);
    }
}
```

---

## Step 4 — Wire in Functionalities bean

In `<Aggregate>Functionalities.java`, add the entry point method:

```java
public XxxDto xxx(Integer aggregateId, ...) {
    String functionalityName = "XxxFunctionality";

    // Sagas path
    SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
    XxxFunctionalitySagas f = new XxxFunctionalitySagas(sagaUnitOfWorkService, aggregateId, ..., uow, commandGateway);
    f.executeWorkflow(uow);
    sagaUnitOfWorkService.commit(uow);
    return f.getResult();
}
```

The Sagas vs. TCC path selection is handled by Spring profiles — inject the appropriate `UnitOfWorkService` variant.

---

## Step 5 — Implement: Command handler

File: `microservices/<aggregate>/commandHandler/local/XxxLocalCommandHandler.java`

```java
@Component
public class XxxLocalCommandHandler implements CommandHandler {
    @Autowired private XxxService service;

    @Override
    public Object handleCommand(Command command) {
        if (command instanceof XxxCommand) {
            XxxCommand cmd = (XxxCommand) command;
            return service.xxx(cmd.getTargetAggregateId(), cmd.getUnitOfWork());
        }
        return null;
    }
}
```

Register in `LocalCommandGateway` or equivalent routing map.

---

## Step 6 — Implement: REST controller (optional)

File: `microservices/<aggregate>/coordination/webapi/<Aggregate>Controller.java`

```java
@RestController
@RequestMapping("/xxx")
public class XxxController {
    @Autowired private XxxFunctionalities functionalities;

    @PostMapping("/{aggregateId}/do-xxx")
    public ResponseEntity<XxxDto> doXxx(@PathVariable Integer aggregateId, @RequestBody XxxDto dto) {
        return ResponseEntity.ok(functionalities.xxx(aggregateId, dto));
    }
}
```

---

## Step 7 — Write tests

Test class: `src/test/groovy/.../sagas/coordination/<aggregate>/XxxTest.groovy`

```groovy
class XxxTest extends QuizzesSpockTest {
    // inject @Autowired dependencies

    def "happy path"() {
        given: "preconditions"
        // use helper methods from QuizzesSpockTest

        when: "execute the functionality"
        functionalities.xxx(aggregateId, ...)

        then: "outcome"
        // assert
    }

    def "concurrent interleaving test"() {
        given: "two concurrent functionalities"
        def f1 = new XxxFunctionalitySagas(...)
        def f2 = new YyyFunctionalitySagas(...)

        when: "interleave steps"
        f1.executeUntilStep("step1", uow1)
        f2.executeWorkflow(uow2)     // f2 completes first
        f1.resumeWorkflow(uow1)      // f1 tries to continue

        then: "expected outcome"
        // thrown(QuizzesException) or assert state
    }
}
```

---

## Checklist

- [ ] Command class created with all needed payload fields
- [ ] `XxxFunctionalitySagas.java` built with correct step dependencies
- [ ] `XxxFunctionalityTCC.java` built (may be structurally identical without forbidden states)
- [ ] Entry point method added to `XxxFunctionalities.java`
- [ ] Command handler routes the command to the service
- [ ] REST controller endpoint (if required)
- [ ] Tests cover happy path, invariant violations, and concurrent interleaving
