package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

public class CourseExecutionDto implements Serializable {
    private Integer aggregateId;
    private Integer courseAggregateId;
    private String name;
    private String type;
    private String acronym;
    private String academicTerm;
    private String endDate;
    private String status;
    private Long version;
    private Long courseVersion;
    private Set<UserDto> students;
    private String state;

    public CourseExecutionDto() {
    }

    public CourseExecutionDto(Execution execution) {
        setAggregateId(execution.getAggregateId());
        setCourseAggregateId(execution.getExecutionCourse().getCourseAggregateId());
        setName(execution.getExecutionCourse().getName());
        setType(execution.getExecutionCourse().getType().toString());
        setAcronym(execution.getAcronym());
        setAcademicTerm(execution.getAcademicTerm());
        setStatus(execution.getState().toString());
        setVersion(execution.getVersion());
        setEndDate(execution.getEndDate().toString());
        setStudents(execution.getStudents().stream().map(CourseExecutionStudent::buildDto).collect(Collectors.toSet()));
        setState(execution.getState().toString());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Long courseVersion) {
        this.courseVersion = courseVersion;
    }

    public Set<UserDto> getStudents() {
        return students;
    }

    public void setStudents(Set<UserDto> students) {
        this.students = students;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
