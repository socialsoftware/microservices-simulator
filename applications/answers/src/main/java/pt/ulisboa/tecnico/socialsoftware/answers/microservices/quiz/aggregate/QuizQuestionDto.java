package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizQuestionDto implements Serializable {
	private Integer questionId;
	private String questionTitle;
	private String questionContent;
	private Integer order;

	public QuizQuestionDto() {
	}

	public QuizQuestionDto(QuizQuestion quizquestion) {
		this.questionId = quizquestion.getQuestionId();
		this.questionTitle = quizquestion.getQuestionTitle();
		this.questionContent = quizquestion.getQuestionContent();
		this.order = quizquestion.getOrder();
	}

	public Integer getQuestionId() {
		return questionId;
	}

	public void setQuestionId(Integer questionId) {
		this.questionId = questionId;
	}

	public String getQuestionTitle() {
		return questionTitle;
	}

	public void setQuestionTitle(String questionTitle) {
		this.questionTitle = questionTitle;
	}

	public String getQuestionContent() {
		return questionContent;
	}

	public void setQuestionContent(String questionContent) {
		this.questionContent = questionContent;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

}