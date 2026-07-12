package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

/**
 * Counts how many tournaments of a course execution a user
 * already participates in.
 * <p>
 * Returns the count rather than the tournaments themselves, this avoids
 * conflicts with command gateway serialization.
 * A scalar result round-trips safely.
 */
public class CountUserTournamentsInExecutionCommand extends Command {
    private final Integer executionAggregateId;
    private final Integer userAggregateId;

    public CountUserTournamentsInExecutionCommand(
            UnitOfWork unitOfWork, String serviceName,
            Integer executionAggregateId, Integer userAggregateId) {

        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }
}
