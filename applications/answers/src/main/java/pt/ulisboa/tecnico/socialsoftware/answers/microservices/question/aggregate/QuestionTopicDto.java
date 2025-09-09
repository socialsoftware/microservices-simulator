package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuestionTopicDto implements Serializable {
	private Integer topicId;
	private String topicName;

	public QuestionTopicDto() {
	}

	public QuestionTopicDto(QuestionTopic questiontopic) {
		this.topicId = questiontopic.getTopicId();
		this.topicName = questiontopic.getTopicName();
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