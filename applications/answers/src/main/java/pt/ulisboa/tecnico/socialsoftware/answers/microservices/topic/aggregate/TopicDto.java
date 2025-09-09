package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TopicDto implements Serializable {
	private Integer aggregateId;
	private String name;
	private Object course;
	private LocalDateTime creationDate;
	private Integer version;
	private AggregateState state;

	public TopicDto() {
	}

	public TopicDto(Topic topic) {
		this.aggregateId = topic.getAggregateId();
		this.name = topic.getName();
		this.course = topic.getCourse();
		this.creationDate = topic.getCreationDate();
		this.version = topic.getVersion();
		this.state = topic.getState();
	}

	public Integer getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(Integer aggregateId) {
		this.aggregateId = aggregateId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object getCourse() {
		return course;
	}

	public void setCourse(Object course) {
		this.course = course;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDateTime creationDate) {
		this.creationDate = creationDate;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public AggregateState getState() {
		return state;
	}

	public void setState(AggregateState state) {
		this.state = state;
	}
}