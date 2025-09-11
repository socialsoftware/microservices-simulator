package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class AnonymizeUserCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer executionAggregateId;
    private final Integer userAggregateId;
    private final String name;
    private final String username;
    private final Integer eventVersion;

    public AnonymizeUserCommand(UnitOfWork unitOfWork,
            String serviceName,
            Integer tournamentAggregateId,
            Integer executionAggregateId,
            Integer userAggregateId,
            String name,
            String username,
            Integer eventVersion) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
        this.name = name;
        this.username = username;
        this.eventVersion = eventVersion;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }
}
