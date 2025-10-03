package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public class ExecutionDto implements Serializable {
    
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private Set<UserDto> students;
    private Integer courseAggregateId;
    private String courseName;
    private String courseType;
    private Integer courseVersion;
    
    public ExecutionDto() {
    }
    
    public ExecutionDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution execution) {
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