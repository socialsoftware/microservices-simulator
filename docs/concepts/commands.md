# Commands

Commands are the messages that cross the boundary between the Functionality layer and the Service layer. Every inter-service call inside a workflow step is expressed as a command dispatched through `CommandGateway`.

---

## What a Command Is

A `Command` subclass carries the inputs for one service operation. It holds the unit of work, the target service name, the primary aggregate ID (passed to `super(...)`), and any domain-specific payload fields.

```java
public class AddParticipantCommand extends Command {
    private Integer tournamentAggregateId;
    private UserDto userDto;

    public AddParticipantCommand(UnitOfWork unitOfWork, String serviceName,
                                  Integer tournamentAggregateId, UserDto userDto) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.userDto = userDto;
    }

    public Integer getTournamentAggregateId() { return tournamentAggregateId; }
    public UserDto getUserDto() { return userDto; }
}
```

`super(unitOfWork, serviceName, aggregateId)`:
- `unitOfWork` — the active `SagaUnitOfWork` for this workflow execution.
- `serviceName` — the `CommandHandler.getAggregateTypeName()` value that routes to the right handler (e.g. `"Tournament"`). Use the `ServiceMapping` enum: `ServiceMapping.TOURNAMENT.getServiceName()`.
- `aggregateId` — the primary aggregate ID; used by the UoW to detect version conflicts.

Commands are plain data carriers — no business logic, no Spring beans.

---

## Naming Conventions

| Purpose | Pattern | Example |
|---------|---------|---------|
| Read command | `Get<Xxx>Command` | `GetTournamentByIdCommand` |
| Mutate command | `<Operation><Xxx>Command` | `AddParticipantCommand`, `RemoveCourseExecutionCommand` |
| Event-driven update | `Update<Field>Command` | `UpdateUserNameCommand` |

---

## File Location

```
src/main/java/<pkg>/<appName>/commands/<aggregate>/
    Get<Xxx>Command.java
    <Operation><Xxx>Command.java
```

All commands for the same target aggregate live in one package under the application root (not inside a microservice package) so they can be shared across services.

---

## ServiceMapping Enum

Each application defines a `ServiceMapping` enum that maps aggregate names to their service routing strings. Use these constants when constructing commands — never hardcode string literals.

```java
public enum ServiceMapping {
    TOURNAMENT("tournament"),
    EXECUTION("execution"),
    // ...
    ;
    private final String serviceName;
    ServiceMapping(String serviceName) { this.serviceName = serviceName; }
    public String getServiceName() { return serviceName; }
}
```

Pass `ServiceMapping.TOURNAMENT.getServiceName()` as the `serviceName` argument to a command constructor.

---

## Sending Commands (Functionality Layer)

Inside a saga step, dispatch a command with `commandGateway.send(...)`. The return type is `Object` — cast to the expected DTO for read commands; ignore it for mutating commands.

```java
// Read step — returns a DTO
SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
    GetCourseExecutionByIdCommand cmd = new GetCourseExecutionByIdCommand(
            unitOfWork,
            ServiceMapping.EXECUTION.getServiceName(),
            executionAggregateId);
    this.executionDto = (CourseExecutionDto) commandGateway.send(cmd);
});

// Mutate step — depends on getExecutionStep; sets forbiddenStates for Sagas
SagaStep addParticipantStep = new SagaStep("addParticipantStep", () -> {
    AddParticipantCommand cmd = new AddParticipantCommand(
            unitOfWork,
            ServiceMapping.TOURNAMENT.getServiceName(),
            tournamentAggregateId,
            this.executionDto);
    cmd.setForbiddenStates(List.of(TournamentSagaState.IN_UPDATE_TOURNAMENT));
    commandGateway.send(cmd);
}, List.of(getExecutionStep));
```

---

## Routing Commands (CommandHandler)

Each aggregate has one `CommandHandler` that receives all commands for that aggregate and dispatches them to the service. Use a `switch` over sealed/pattern-matched types.

```java
@Component
public class TournamentCommandHandler extends CommandHandler {

    @Autowired
    private TournamentService tournamentService;

    @Override
    public String getAggregateTypeName() {
        return "Tournament";   // must match ServiceMapping value
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetTournamentByIdCommand cmd -> tournamentService.getTournamentById(
                    cmd.getAggregateId(), cmd.getUnitOfWork());
            case AddParticipantCommand cmd -> {
                tournamentService.addParticipant(
                        cmd.getTournamentAggregateId(), cmd.getUserDto(), cmd.getUnitOfWork());
                yield null;
            }
            // ... one case per command
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
```

`getAggregateTypeName()` must return the same string used as `serviceName` in the command constructor and in `ServiceMapping`.

Mutating handlers return `null`; read handlers return the DTO produced by the service method.

---

## Reference Implementations (Quizzes)

- `applications/quizzes/src/main/java/.../quizzes/commands/tournament/AddParticipantCommand.java` — mutate command with a DTO payload
- `applications/quizzes/src/main/java/.../quizzes/commands/tournament/GetTournamentByIdCommand.java` — read command
- `applications/quizzes/src/main/java/.../quizzes/microservices/tournament/messaging/TournamentCommandHandler.java` — full handler with pattern switch
- `applications/quizzes/src/main/java/.../quizzes/ServiceMapping.java` — service name enum
