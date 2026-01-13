package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionCourseDto;
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

    public ExecutionCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
        setCourseName(courseDto.getName());
        setCourseType(CourseType.valueOf(courseDto.getType()));
    }

    public ExecutionCourse(ExecutionCourse other) {
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


    public ExecutionCourseDto buildDto() {
        ExecutionCourseDto dto = new ExecutionCourseDto();
        dto.setAggregateId(getCourseAggregateId());
        dto.setName(getCourseName());
        dto.setType(getCourseType() != null ? getCourseType().name() : null);
        dto.setVersion(getCourseVersion());
        return dto;
    }
}