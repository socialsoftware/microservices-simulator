package pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class UpdateUserNameCommand extends Command {
    private Integer answerAggregateId;
    private Integer executionAggregateId;
    private Integer eventVersion;
    private Integer userAggregateId;
    private String name;

    public UpdateUserNameCommand(UnitOfWork unitOfWork, String serviceName, Integer answerAggregateId, Integer executionAggregateId, Integer eventVersion, Integer userAggregateId, String name) {
        super(unitOfWork, serviceName, answerAggregateId);
        this.answerAggregateId = answerAggregateId;
        this.executionAggregateId = executionAggregateId;
        this.eventVersion = eventVersion;
        this.userAggregateId = userAggregateId;
        this.name = name;
    }

    public Integer getAnswerAggregateId() { return answerAggregateId; }
    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public Integer getEventVersion() { return eventVersion; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public String getName() { return name; }
}
