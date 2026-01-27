package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;

@Entity
public class ExecutionCourse {
    @Id
    @GeneratedValue
    private Long id;
    private String courseName;
    @Enumerated(EnumType.STRING)
    private CourseType courseType;
    private Integer courseAggregateId;
    private Integer courseVersion;
    private AggregateState courseState;
    @OneToOne
    private Execution execution;

    public ExecutionCourse() {

    }

    public ExecutionCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
        setCourseState(courseDto.getState());
        setCourseName(courseDto.getName());
        setCourseType(CourseType.valueOf(courseDto.getType()));
    }

    public ExecutionCourse(ExecutionCourseDto executionCourseDto) {
        setCourseName(executionCourseDto.getName());
        setCourseType(executionCourseDto.getType() != null ? CourseType.valueOf(executionCourseDto.getType()) : null);
        setCourseAggregateId(executionCourseDto.getAggregateId());
        setCourseVersion(executionCourseDto.getVersion());
        setCourseState(executionCourseDto.getState());
    }

    public ExecutionCourse(ExecutionCourse other) {
        setCourseType(other.getCourseType());
        setCourseAggregateId(other.getCourseAggregateId());
        setCourseVersion(other.getCourseVersion());
        setCourseState(other.getCourseState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

    public AggregateState getCourseState() {
        return courseState;
    }

    public void setCourseState(AggregateState courseState) {
        this.courseState = courseState;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }



    public ExecutionCourseDto buildDto() {
        ExecutionCourseDto dto = new ExecutionCourseDto();
        dto.setName(getCourseName());
        dto.setType(getCourseType() != null ? getCourseType().name() : null);
        dto.setAggregateId(getCourseAggregateId());
        dto.setVersion(getCourseVersion());
        dto.setState(getCourseState());
        return dto;
    }
}