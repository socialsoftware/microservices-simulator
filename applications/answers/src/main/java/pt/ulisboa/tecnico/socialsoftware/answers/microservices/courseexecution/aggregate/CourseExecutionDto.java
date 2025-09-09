package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class CourseExecutionDto implements Serializable {
	private Integer aggregateId;
	private String name;
	private String acronym;
	private String academicTerm;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private Object course;
	private Object students;
	private Integer version;
	private AggregateState state;

	public CourseExecutionDto() {
	}

	public CourseExecutionDto(CourseExecution courseexecution) {
		this.aggregateId = courseexecution.getAggregateId();
		this.name = courseexecution.getName();
		this.acronym = courseexecution.getAcronym();
		this.academicTerm = courseexecution.getAcademicTerm();
		this.startDate = courseexecution.getStartDate();
		this.endDate = courseexecution.getEndDate();
		this.course = courseexecution.getCourse();
		this.students = courseexecution.getStudents();
		this.version = courseexecution.getVersion();
		this.state = courseexecution.getState();
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

	public String getAcademicTerm() {
		return academicTerm;
	}

	public void setAcademicTerm(String academicTerm) {
		this.academicTerm = academicTerm;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDateTime startDate) {
		this.startDate = startDate;
	}

	public LocalDateTime getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDateTime endDate) {
		this.endDate = endDate;
	}

	public Object getCourse() {
		return course;
	}

	public void setCourse(Object course) {
		this.course = course;
	}

	public Object getStudents() {
		return students;
	}

	public void setStudents(Object students) {
		this.students = students;
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