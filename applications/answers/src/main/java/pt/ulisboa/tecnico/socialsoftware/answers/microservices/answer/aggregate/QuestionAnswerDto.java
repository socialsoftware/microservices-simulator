package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuestionAnswerDto implements Serializable {
	private Integer questionId;
	private String answer;
	private String option;
	private LocalDateTime answerDate;

	public QuestionAnswerDto() {
	}

	public QuestionAnswerDto(QuestionAnswer questionanswer) {
		this.questionId = questionanswer.getQuestionId();
		this.answer = questionanswer.getAnswer();
		this.option = questionanswer.getOption();
		this.answerDate = questionanswer.getAnswerDate();
	}

	public Integer getQuestionId() {
		return questionId;
	}

	public void setQuestionId(Integer questionId) {
		this.questionId = questionId;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getOption() {
		return option;
	}

	public void setOption(String option) {
		this.option = option;
	}

	public LocalDateTime getAnswerDate() {
		return answerDate;
	}

	public void setAnswerDate(LocalDateTime answerDate) {
		this.answerDate = answerDate;
	}

}