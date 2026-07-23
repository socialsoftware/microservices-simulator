package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;

import java.util.Set;

public class UpdateQuestionCommand extends Command {
    private final Integer questionAggregateId;
    private final String title;
    private final String content;
    private final Set<QuestionTopic> topics;

    public UpdateQuestionCommand(UnitOfWork unitOfWork, String serviceName,
                                 Integer questionAggregateId, String title, String content,
                                 Set<QuestionTopic> topics) {
        super(unitOfWork, serviceName, questionAggregateId);
        this.questionAggregateId = questionAggregateId;
        this.title = title;
        this.content = content;
        this.topics = topics;
    }

    public Integer getQuestionAggregateId() { return questionAggregateId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Set<QuestionTopic> getTopics() { return topics; }
}
