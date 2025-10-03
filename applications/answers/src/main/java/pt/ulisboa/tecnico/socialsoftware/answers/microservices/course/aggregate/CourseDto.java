package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class CourseDto implements Serializable {
	private Integer aggregateId;
	private String name;
	private String acronym;
	private String courseType;
	private LocalDateTime creationDate;
	private Integer version;
	private AggregateState state;

	public CourseDto() {
	}

	public CourseDto(Course course) {
		this.aggregateId = course.getAggregateId();
		this.name = course.getName();
		this.acronym = course.getAcronym();
		this.courseType = course.getCourseType() != null ? course.getCourseType().toString() : null;
		this.creationDate = course.getCreationDate();
		this.version = course.getVersion();
		this.state = course.getState();
	}

	public Integer getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(Integer aggregateId) {
		this.aggregateId = aggregateId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAcronym() {
		return acronym;
	}

	public void setAcronym(String acronym) {
		this.acronym = acronym;
	}

	public String getCourseType() {
		return courseType;
	}

	public void setCourseType(String courseType) {
		this.courseType = courseType;
	}

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(LocalDateTime creationDate) {
		this.creationDate = creationDate;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public AggregateState getState() {
		return state;
	}

	public void setState(AggregateState state) {
		this.state = state;
	}
}