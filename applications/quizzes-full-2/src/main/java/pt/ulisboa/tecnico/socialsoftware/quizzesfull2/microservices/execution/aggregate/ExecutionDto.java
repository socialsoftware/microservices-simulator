package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionDto {
    private Integer aggregateId;
    private Long version;
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private Integer courseAggregateId;
    private String courseName;
    private CourseType courseType;
    private List<ExecutionStudentDto> students = new ArrayList<>();

    public ExecutionDto() {
    }

    public ExecutionDto(Integer aggregateId, Long version, String acronym, String academicTerm, LocalDateTime endDate,
                        Integer courseAggregateId, String courseName, CourseType courseType,
                        List<ExecutionStudentDto> students) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.acronym = acronym;
        this.academicTerm = academicTerm;
        this.endDate = endDate;
        this.courseAggregateId = courseAggregateId;
        this.courseName = courseName;
        this.courseType = courseType;
        this.students = students;
    }

    public ExecutionDto(Execution execution) {
        this.aggregateId = execution.getAggregateId();
        this.version = execution.getVersion();
        this.acronym = execution.getAcronym();
        this.academicTerm = execution.getAcademicTerm();
        this.endDate = execution.getEndDate();
        this.courseAggregateId = execution.getCourseAggregateId();
        this.courseName = execution.getCourseName();
        this.courseType = execution.getCourseType();
        this.students = execution.getStudents().stream()
                .map(ExecutionStudentDto::new)
                .collect(Collectors.toList());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public List<ExecutionStudentDto> getStudents() {
        return students;
    }

    public void setStudents(List<ExecutionStudentDto> students) {
        this.students = students;
    }
}
