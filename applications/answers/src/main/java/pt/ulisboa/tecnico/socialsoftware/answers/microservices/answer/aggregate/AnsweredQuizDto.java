package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class AnsweredQuizDto implements Serializable {
	private Integer quizAggregateId;
	private String quizTitle;
	private String quizType;
	private LocalDateTime availableDate;
	private LocalDateTime conclusionDate;
	private Integer numberOfQuestions;

	public AnsweredQuizDto() {
	}

	public AnsweredQuizDto(AnsweredQuiz answeredquiz) {
		this.quizAggregateId = answeredquiz.getQuizAggregateId();
		this.quizTitle = answeredquiz.getQuizTitle();
		this.quizType = answeredquiz.getQuizType();
		this.availableDate = answeredquiz.getAvailableDate();
		this.conclusionDate = answeredquiz.getConclusionDate();
		this.numberOfQuestions = answeredquiz.getNumberOfQuestions();
	}

	public Integer getQuizAggregateId() {
		return quizAggregateId;
	}

	public void setQuizAggregateId(Integer quizAggregateId) {
		this.quizAggregateId = quizAggregateId;
	}

	public String getQuizTitle() {
		return quizTitle;
	}

	public void setQuizTitle(String quizTitle) {
		this.quizTitle = quizTitle;
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

}