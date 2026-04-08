package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class UpdateUserNameCommand extends Command {
    private final Integer tournamentAggregateId;
    private final Integer executionAggregateId;
    private final Long eventVersion;
    private final Integer userAggregateId;
    private final String name;

    public UpdateUserNameCommand(UnitOfWork unitOfWork,
            String serviceName,
            Integer tournamentAggregateId,
            Integer executionAggregateId,
            Long eventVersion,
            Integer userAggregateId,
            String name) {
        super(unitOfWork, serviceName, tournamentAggregateId);
        this.tournamentAggregateId = tournamentAggregateId;
        this.executionAggregateId = executionAggregateId;
        this.eventVersion = eventVersion;
        this.userAggregateId = userAggregateId;
        this.name = name;
    }

    public Integer getTournamentAggregateId() {
        return tournamentAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Long getEventVersion() {
        return eventVersion;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public String getName() {
        return name;
    }
}
