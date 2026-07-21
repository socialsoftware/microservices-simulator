package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;

@Entity
public class ExecutionCourse {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer courseAggregateId;
    private String courseName;
    private String courseType;
    @OneToOne
    private Execution execution;

    public ExecutionCourse() {}

    public ExecutionCourse(CourseDto courseDto) {
        this.courseAggregateId = courseDto.getAggregateId();
        this.courseName = courseDto.getName();
        this.courseType = courseDto.getType();
    }

    public ExecutionCourse(ExecutionCourse other) {
        this.courseAggregateId = other.getCourseAggregateId();
        this.courseName = other.getCourseName();
        this.courseType = other.getCourseType();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCourseAggregateId() { return courseAggregateId; }
    public void setCourseAggregateId(Integer courseAggregateId) { this.courseAggregateId = courseAggregateId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    @JsonIgnore
    public Execution getExecution() { return execution; }
    public void setExecution(Execution execution) { this.execution = execution; }
}
