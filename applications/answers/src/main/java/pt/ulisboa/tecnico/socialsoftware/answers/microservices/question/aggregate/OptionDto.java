package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class OptionDto implements Serializable {
	private Integer optionNumber;
	private String content;
	private boolean isCorrect;

	public OptionDto() {
	}

	public OptionDto(Option option) {
		this.optionNumber = option.getOptionNumber();
		this.content = option.getContent();
		this.isCorrect = option.isIsCorrect();
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