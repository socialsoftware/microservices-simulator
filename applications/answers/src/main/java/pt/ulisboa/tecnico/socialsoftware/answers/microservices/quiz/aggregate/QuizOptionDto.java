package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizOptionDto implements Serializable {
	private Integer optionNumber;
	private String content;
	private boolean isCorrect;

	public QuizOptionDto() {
	}

	public QuizOptionDto(QuizOption quizoption) {
		this.optionNumber = quizoption.getOptionNumber();
		this.content = quizoption.getContent();
		this.isCorrect = quizoption.isIsCorrect();
	}

	public Integer getOptionNumber() {
		return optionNumber;
	}

	public void setOptionNumber(Integer optionNumber) {
		this.optionNumber = optionNumber;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isIsCorrect() {
		return isCorrect;
	}

	public void setIsCorrect(Boolean isCorrect) {
		this.isCorrect = isCorrect;
	}

}