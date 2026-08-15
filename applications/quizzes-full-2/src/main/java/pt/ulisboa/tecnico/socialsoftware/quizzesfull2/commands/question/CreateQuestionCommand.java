package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopicDto;

import java.util.List;

public class CreateQuestionCommand extends Command {
    private QuestionDto questionDto;
    private List<QuestionTopicDto> topics;

    protected CreateQuestionCommand() {}

    public CreateQuestionCommand(UnitOfWork unitOfWork, String serviceName, QuestionDto questionDto,
                                 List<QuestionTopicDto> topics) {
        super(unitOfWork, serviceName, null);
        this.questionDto = questionDto;
        this.topics = topics;
    }

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }

    public List<QuestionTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<QuestionTopicDto> topics) {
        this.topics = topics;
    }
}
