package pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class GetTournamentsByCourseExecutionIdCommand extends Command {
    private final Integer executionAggregateId;

    public GetTournamentsByCourseExecutionIdCommand(UnitOfWork unitOfWork, String serviceName,
            Integer executionAggregateId) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }
}
