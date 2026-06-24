package pt.ulisboa.tecnico.socialsoftware.quizzes.commands.question;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionTopic;

import java.util.Set;

public class UpdateQuestionTopicsCommand extends Command {
    private Integer courseAggregateId;
    private Set<QuestionTopic> topics;

    public UpdateQuestionTopicsCommand(UnitOfWork unitOfWork, String serviceName, Integer courseAggregateId, Set<QuestionTopic> topics) {
        super(unitOfWork, serviceName, null);
        this.courseAggregateId = courseAggregateId;
        this.topics = topics;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public Set<QuestionTopic> getTopics() {
        return topics;
    }
}
