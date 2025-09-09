package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuestionCourseDto implements Serializable {
	private Integer courseAggregateId;
	private String courseName;
	private String courseAcronym;

	public QuestionCourseDto() {
	}

	public QuestionCourseDto(QuestionCourse questioncourse) {
		this.courseAggregateId = questioncourse.getCourseAggregateId();
		this.courseName = questioncourse.getCourseName();
		this.courseAcronym = questioncourse.getCourseAcronym();
	}

	public Integer getCourseAggregateId() {
		return courseAggregateId;
	}

	public void setCourseAggregateId(Integer courseAggregateId) {
		this.courseAggregateId = courseAggregateId;
	}

	public String getCourseName() {
		return courseName;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}

	public String getCourseAcronym() {
		return courseAcronym;
	}

	public void setCourseAcronym(String courseAcronym) {
		this.courseAcronym = courseAcronym;
	}

}