package pt.ulisboa.tecnico.socialsoftware.answers.command.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import java.util.List;

public class AddQuestionTopicsCommand extends Command {
    private final Integer questionId;
    private final List<QuestionTopicDto> topicDtos;

    public AddQuestionTopicsCommand(UnitOfWork unitOfWork, String serviceName, Integer questionId, List<QuestionTopicDto> topicDtos) {
        super(unitOfWork, serviceName, null);
        this.questionId = questionId;
        this.topicDtos = topicDtos;
    }

    public Integer getQuestionId() { return questionId; }
    public List<QuestionTopicDto> getTopicDtos() { return topicDtos; }
}
