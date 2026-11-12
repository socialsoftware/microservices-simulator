package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetQuizzesForExecutionCommand extends Command {
    private Integer executionAggregateId;

    protected GetQuizzesForExecutionCommand() {}

    public GetQuizzesForExecutionCommand(UnitOfWork unitOfWork, String serviceName,
                                         Integer executionAggregateId) {
        super(unitOfWork, serviceName, null);
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }
}
