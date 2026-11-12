package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopicDto;

import java.util.List;

public class UpdateQuestionCommand extends Command {
    private Integer questionAggregateId;
    private String title;
    private String content;
    private List<QuestionTopicDto> topics;

    protected UpdateQuestionCommand() {}

    public UpdateQuestionCommand(UnitOfWork unitOfWork, String serviceName, Integer questionAggregateId,
                                 String title, String content, List<QuestionTopicDto> topics) {
        super(unitOfWork, serviceName, questionAggregateId);
        this.questionAggregateId = questionAggregateId;
        this.title = title;
        this.content = content;
        this.topics = topics;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<QuestionTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<QuestionTopicDto> topics) {
        this.topics = topics;
    }
}
