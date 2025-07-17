package pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class UpdateExecutionStudentNameCommand extends Command {
    private Integer executionAggregateId;
    private Integer userAggregateId;
    private String name;

    public UpdateExecutionStudentNameCommand(UnitOfWork unitOfWork, String serviceName, Integer executionAggregateId, Integer userAggregateId, String name) {
        super(unitOfWork, serviceName, executionAggregateId);
        this.executionAggregateId = executionAggregateId;
        this.userAggregateId = userAggregateId;
        this.name = name;
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
}
