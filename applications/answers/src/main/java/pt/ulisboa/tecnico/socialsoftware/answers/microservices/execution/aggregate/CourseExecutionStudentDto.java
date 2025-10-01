package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class CourseExecutionStudentDto implements Serializable {
	private Integer studentAggregateId;
	private String studentName;
	private String studentUsername;
	private String studentEmail;
	private LocalDateTime enrollmentDate;

	public CourseExecutionStudentDto() {
	}

	public CourseExecutionStudentDto(CourseExecutionStudent courseexecutionstudent) {
		this.studentAggregateId = courseexecutionstudent.getStudentAggregateId();
		this.studentName = courseexecutionstudent.getStudentName();
		this.studentUsername = courseexecutionstudent.getStudentUsername();
		this.studentEmail = courseexecutionstudent.getStudentEmail();
		this.enrollmentDate = courseexecutionstudent.getEnrollmentDate();
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

	public LocalDateTime getEnrollmentDate() {
		return enrollmentDate;
	}

	public void setEnrollmentDate(LocalDateTime enrollmentDate) {
		this.enrollmentDate = enrollmentDate;
	}

}