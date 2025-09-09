package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class QuestionTopic {
    private Integer topicId;
    private String topicName; 

    public QuestionTopic(Integer topicId, String topicName) {
        this.topicId = topicId;
        this.topicName = topicName;
    }

    public QuestionTopic(QuestionTopic other) {
        // Copy constructor
    }


    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }


}