package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizDto implements Serializable {
	private Integer aggregateId;
	private String title;
	private String description;
	private String quizType;
	private LocalDateTime availableDate;
	private LocalDateTime conclusionDate;
	private Integer numberOfQuestions;
	private Object courseExecution;
	private Object questions;
	private Object options;
	private Integer version;
	private AggregateState state;

	public QuizDto() {
	}

	public QuizDto(Quiz quiz) {
		this.aggregateId = quiz.getAggregateId();
		this.title = quiz.getTitle();
		this.description = quiz.getDescription();
		this.quizType = quiz.getQuizType();
		this.availableDate = quiz.getAvailableDate();
		this.conclusionDate = quiz.getConclusionDate();
		this.numberOfQuestions = quiz.getNumberOfQuestions();
		this.courseExecution = quiz.getCourseExecution();
		this.questions = quiz.getQuestions();
		this.options = quiz.getOptions();
		this.version = quiz.getVersion();
		this.state = quiz.getState();
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getQuizType() {
		return quizType;
	}

	public void setQuizType(String quizType) {
		this.quizType = quizType;
	}

	public LocalDateTime getAvailableDate() {
		return availableDate;
	}

	public void setAvailableDate(LocalDateTime availableDate) {
		this.availableDate = availableDate;
	}

	public LocalDateTime getConclusionDate() {
		return conclusionDate;
	}

	public void setConclusionDate(LocalDateTime conclusionDate) {
		this.conclusionDate = conclusionDate;
	}

	public Integer getNumberOfQuestions() {
		return numberOfQuestions;
	}

	public void setNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

	public Object getCourseExecution() {
		return courseExecution;
	}

	public void setCourseExecution(Object courseExecution) {
		this.courseExecution = courseExecution;
	}

	public Object getQuestions() {
		return questions;
	}

	public void setQuestions(Object questions) {
		this.questions = questions;
	}

	public Object getOptions() {
		return options;
	}

	public void setOptions(Object options) {
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