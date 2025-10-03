package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;

@Entity
public class ExecutionCourse {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseAggregateId;
    private String courseName;
    @Enumerated(EnumType.STRING)
    private CourseType courseType;
    private Integer courseVersion;
    @OneToOne
    private Execution execution; 

    public ExecutionCourse() {
    }

    public ExecutionCourse(ExecutionDto executionDto) {
        setCourseAggregateId(executionDto.getCourseAggregateId());
        setCourseName(executionDto.getCourseName());
        setCourseType(CourseType.valueOf(executionDto.getCourseType()));
        setCourseVersion(executionDto.getCourseVersion());
    }

    public ExecutionCourse(ExecutionCourse other) {
        setCourseAggregateId(other.getCourseAggregateId());
        setCourseName(other.getCourseName());
        setCourseType(other.getCourseType());
        setCourseVersion(other.getCourseVersion());
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }


}