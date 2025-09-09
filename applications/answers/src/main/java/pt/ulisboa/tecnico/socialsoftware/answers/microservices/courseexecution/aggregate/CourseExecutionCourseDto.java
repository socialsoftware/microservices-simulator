package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class CourseExecutionCourseDto implements Serializable {
	private Integer courseAggregateId;
	private String courseName;
	private String courseAcronym;
	private String courseType;

	public CourseExecutionCourseDto() {
	}

	public CourseExecutionCourseDto(CourseExecutionCourse courseexecutioncourse) {
		this.courseAggregateId = courseexecutioncourse.getCourseAggregateId();
		this.courseName = courseexecutioncourse.getCourseName();
		this.courseAcronym = courseexecutioncourse.getCourseAcronym();
		this.courseType = courseexecutioncourse.getCourseType();
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

	public String getCourseType() {
		return courseType;
	}

	public void setCourseType(String courseType) {
		this.courseType = courseType;
	}

}