package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizExecutionDto implements Serializable {
	private Integer executionAggregateId;
	private String executionName;
	private String executionAcronym;
	private String executionAcademicTerm;

	public QuizExecutionDto() {
	}

	public QuizExecutionDto(QuizExecution quizexecution) {
		this.executionAggregateId = quizexecution.getExecutionAggregateId();
		this.executionName = quizexecution.getExecutionName();
		this.executionAcronym = quizexecution.getExecutionAcronym();
		this.executionAcademicTerm = quizexecution.getExecutionAcademicTerm();
	}

	public Integer getExecutionAggregateId() {
		return executionAggregateId;
	}

	public void setExecutionAggregateId(Integer executionAggregateId) {
		this.executionAggregateId = executionAggregateId;
	}

	public String getExecutionName() {
		return executionName;
	}

	public void setExecutionName(String executionName) {
		this.executionName = executionName;
	}

	public String getExecutionAcronym() {
		return executionAcronym;
	}

	public void setExecutionAcronym(String executionAcronym) {
		this.executionAcronym = executionAcronym;
	}

	public String getExecutionAcademicTerm() {
		return executionAcademicTerm;
	}

	public void setExecutionAcademicTerm(String executionAcademicTerm) {
		this.executionAcademicTerm = executionAcademicTerm;
	}

}