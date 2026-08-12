package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ExecutionDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private Integer courseId;
    private String courseName;
    private String courseType;

    public ExecutionDto() {}

    public ExecutionDto(Execution execution) {
        this.aggregateId = execution.getAggregateId();
        this.version = execution.getVersion();
        this.state = execution.getState();
        this.acronym = execution.getAcronym();
        this.academicTerm = execution.getAcademicTerm();
        this.endDate = execution.getEndDate();
        if (execution.getExecutionCourse() != null) {
            this.courseId = execution.getExecutionCourse().getCourseAggregateId();
            this.courseName = execution.getExecutionCourse().getCourseName();
            this.courseType = execution.getExecutionCourse().getCourseType();
        }
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public AggregateState getState() { return state; }
    public void setState(AggregateState state) { this.state = state; }

    public String getAcronym() { return acronym; }
    public void setAcronym(String acronym) { this.acronym = acronym; }

    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }
}
