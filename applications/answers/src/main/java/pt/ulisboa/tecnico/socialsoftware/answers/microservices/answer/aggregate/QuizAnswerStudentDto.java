package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizAnswerStudentDto implements Serializable {
	private Integer studentAggregateId;
	private String studentName;
	private String studentUsername;
	private String studentEmail;

	public QuizAnswerStudentDto() {
	}

	public QuizAnswerStudentDto(QuizAnswerStudent quizanswerstudent) {
		this.studentAggregateId = quizanswerstudent.getStudentAggregateId();
		this.studentName = quizanswerstudent.getStudentName();
		this.studentUsername = quizanswerstudent.getStudentUsername();
		this.studentEmail = quizanswerstudent.getStudentEmail();
	}

	public Integer getStudentAggregateId() {
		return studentAggregateId;
	}

	public void setStudentAggregateId(Integer studentAggregateId) {
		this.studentAggregateId = studentAggregateId;
	}

	public String getStudentName() {
		return studentName;
	}

	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}

	public String getStudentUsername() {
		return studentUsername;
	}

	public void setStudentUsername(String studentUsername) {
		this.studentUsername = studentUsername;
	}

	public String getStudentEmail() {
		return studentEmail;
	}

	public void setStudentEmail(String studentEmail) {
		this.studentEmail = studentEmail;
	}

}