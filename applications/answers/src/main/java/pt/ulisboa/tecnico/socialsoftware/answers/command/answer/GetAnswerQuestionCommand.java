package pt.ulisboa.tecnico.socialsoftware.answers.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetAnswerQuestionCommand extends Command {
    private final Integer answerId;
    private final Integer questionAggregateId;

    public GetAnswerQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer answerId, Integer questionAggregateId) {
        super(unitOfWork, serviceName, null);
        this.answerId = answerId;
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getAnswerId() { return answerId; }
    public Integer getQuestionAggregateId() { return questionAggregateId; }
}
