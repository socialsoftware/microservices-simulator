package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.io.Serializable;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuestionDto implements Serializable {
	private Integer aggregateId;
	private String title;
	private String content;
	private Integer numberOfOptions;
	private Integer correctOption;
	private Integer order;
	private QuestionCourse course;
	private Set<QuestionTopic> topics;
	private Set<Option> options;
	private Integer version;
	private AggregateState state;

	public QuestionDto() {
	}

	public QuestionDto(Question question) {
		this.aggregateId = question.getAggregateId();
		this.title = question.getTitle();
		this.content = question.getContent();
		this.numberOfOptions = question.getNumberOfOptions();
		this.correctOption = question.getCorrectOption();
		this.order = question.getOrder();
		this.course = question.getCourse();
		this.topics = question.getTopics();
		this.options = question.getOptions();
		this.version = question.getVersion();
		this.state = question.getState();
	}

	public Integer getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(Integer aggregateId) {
		this.aggregateId = aggregateId;
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

	public Integer getNumberOfOptions() {
		return numberOfOptions;
	}

	public void setNumberOfOptions(Integer numberOfOptions) {
		this.numberOfOptions = numberOfOptions;
	}

	public Integer getCorrectOption() {
		return correctOption;
	}

	public void setCorrectOption(Integer correctOption) {
		this.correctOption = correctOption;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public QuestionCourse getCourse() {
		return course;
	}

	public void setCourse(QuestionCourse course) {
		this.course = course;
	}

	public Set<QuestionTopic> getTopics() {
		return topics;
	}

	public void setTopics(Set<QuestionTopic> topics) {
		this.topics = topics;
	}

	public Set<Option> getOptions() {
		return options;
	}

	public void setOptions(Set<Option> options) {
		this.options = options;
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