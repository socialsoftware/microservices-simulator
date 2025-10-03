package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class ExecutionDto implements Serializable {
    
    // Standard aggregate fields
    private Integer aggregateId;
    private Integer version;
    private String state;

    // Root entity fields
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private Set<UserDto> students;

    // Fields from ExecutionCourse
    private Integer courseAggregateId;
    private String courseName;
    private String courseType;
    private Integer courseVersion;
    
    public ExecutionDto() {
    }
    
    public ExecutionDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution execution) {
        // Standard aggregate fields
        setAggregateId(execution.getAggregateId());
        setVersion(execution.getVersion());
        setState(execution.getState().toString());

        // Root entity fields
        setAcronym(execution.getAcronym());
        setAcademicTerm(execution.getAcademicTerm());
        setEndDate(execution.getEndDate());
        setStudents(execution.getStudents().stream()
            .map(executionstudent -> new UserDto(executionstudent.getStudentAggregateId(), executionstudent.getStudentName(), executionstudent.getStudentUsername(), executionstudent.getStudentEmail()))
            .collect(Collectors.toSet()));

        // Fields from ExecutionCourse
        setCourseAggregateId(execution.getExecutionCourse().getCourseAggregateId());
        setCourseName(execution.getExecutionCourse().getCourseName());
        setCourseType(execution.getExecutionCourse().getCourseType().toString());
        setCourseVersion(execution.getExecutionCourse().getCourseVersion());

    }
    
    public Integer getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
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

    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Set<UserDto> getStudents() {
        return students;
    }
    
    public void setStudents(Set<UserDto> students) {
        this.students = students;
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

    public String getCourseType() {
        return courseType;
    }
    
    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }
    
    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }
}