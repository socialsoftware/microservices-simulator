package pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class AnonymizeStudentCommand extends Command {
    private Integer executionAggregateId;
    private Integer userAggregateId;

    public AnonymizeStudentCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId, Integer userAggregateId) {
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
