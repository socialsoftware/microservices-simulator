package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;

public class UpdateQuestionTopicCommand extends Command {
    private final Integer questionId;
    private final Integer topicAggregateId;
    private final QuestionTopicDto topicDto;

    public UpdateQuestionTopicCommand(UnitOfWork unitOfWork, String serviceName, Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto) {
        super(unitOfWork, serviceName, null);
        this.questionId = questionId;
        this.topicAggregateId = topicAggregateId;
        this.topicDto = topicDto;
    }

    public Integer getQuestionId() { return questionId; }
    public Integer getTopicAggregateId() { return topicAggregateId; }
    public QuestionTopicDto getTopicDto() { return topicDto; }
}
