package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class UpdateUserNameCommand extends Command {
    private Integer answerAggregateId;
    private Integer executionAggregateId;
    private Long eventVersion;
    private Integer userAggregateId;
    private String name;

    public UpdateUserNameCommand(UnitOfWork unitOfWork, String serviceName, Integer answerAggregateId, Integer executionAggregateId, Long eventVersion, Integer userAggregateId, String name) {
        super(unitOfWork, serviceName, answerAggregateId);
        this.answerAggregateId = answerAggregateId;
        this.executionAggregateId = executionAggregateId;
        this.eventVersion = eventVersion;
        this.userAggregateId = userAggregateId;
        this.name = name;
    }

    public Integer getAnswerAggregateId() { return answerAggregateId; }
    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public Long getEventVersion() { return eventVersion; }
    public Integer getUserAggregateId() { return userAggregateId; }
    public String getName() { return name; }
}
