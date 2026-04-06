package pt.ulisboa.tecnico.socialsoftware.answers.command.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;

public class UpdateAnswerQuestionCommand extends Command {
    private final Integer answerId;
    private final Integer questionAggregateId;
    private final AnswerQuestionDto questionDto;

    public UpdateAnswerQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto) {
        super(unitOfWork, serviceName, null);
        this.answerId = answerId;
        this.questionAggregateId = questionAggregateId;
        this.questionDto = questionDto;
    }

    public Integer getAnswerId() { return answerId; }
    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public AnswerQuestionDto getQuestionDto() { return questionDto; }
}
