package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizAnswerCourseExecutionDto implements Serializable {
	private Integer courseExecutionAggregateId;
	private String courseExecutionName;
	private String courseExecutionAcronym;
	private String courseExecutionAcademicTerm;

	public QuizAnswerCourseExecutionDto() {
	}

	public QuizAnswerCourseExecutionDto(QuizAnswerCourseExecution quizanswercourseexecution) {
		this.courseExecutionAggregateId = quizanswercourseexecution.getCourseExecutionAggregateId();
		this.courseExecutionName = quizanswercourseexecution.getCourseExecutionName();
		this.courseExecutionAcronym = quizanswercourseexecution.getCourseExecutionAcronym();
		this.courseExecutionAcademicTerm = quizanswercourseexecution.getCourseExecutionAcademicTerm();
	}

	public Integer getCourseExecutionAggregateId() {
		return courseExecutionAggregateId;
	}

	public void setCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
		this.courseExecutionAggregateId = courseExecutionAggregateId;
	}

	public String getCourseExecutionName() {
		return courseExecutionName;
	}

	public void setCourseExecutionName(String courseExecutionName) {
		this.courseExecutionName = courseExecutionName;
	}

	public String getCourseExecutionAcronym() {
		return courseExecutionAcronym;
	}

	public void setCourseExecutionAcronym(String courseExecutionAcronym) {
		this.courseExecutionAcronym = courseExecutionAcronym;
	}

	public String getCourseExecutionAcademicTerm() {
		return courseExecutionAcademicTerm;
	}

	public void setCourseExecutionAcademicTerm(String courseExecutionAcademicTerm) {
		this.courseExecutionAcademicTerm = courseExecutionAcademicTerm;
	}

}