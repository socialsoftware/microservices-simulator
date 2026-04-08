package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class RemoveCourseExecutionCommand extends Command {
    private Integer executionAggregateId;

    public RemoveCourseExecutionCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }
}
