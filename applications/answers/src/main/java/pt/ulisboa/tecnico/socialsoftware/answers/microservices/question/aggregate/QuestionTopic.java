package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

@Entity
public class QuestionTopic {
    @Id
    @GeneratedValue
    private Integer topicId;
    private String topicName;
    @OneToOne
    private Question question;

    public QuestionTopic() {
    }

    public QuestionTopic(TopicDto topicDto) {
        setTopicName(topicDto.getTopicName());
    }

    public QuestionTopic(QuestionTopic other) {
        setTopicName(other.getTopicName());
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

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

}