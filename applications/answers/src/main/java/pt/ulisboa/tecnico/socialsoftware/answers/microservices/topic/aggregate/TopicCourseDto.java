package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TopicCourseDto implements Serializable {
	private Integer courseAggregateId;
	private String courseName;
	private String courseAcronym;

	public TopicCourseDto() {
	}

	public TopicCourseDto(TopicCourse topiccourse) {
		this.courseAggregateId = topiccourse.getCourseAggregateId();
		this.courseName = topiccourse.getCourseName();
		this.courseAcronym = topiccourse.getCourseAcronym();
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