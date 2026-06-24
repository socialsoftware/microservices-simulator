package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseType;

@Entity
public class CourseExecutionCourse {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseAggregateId;
    private String name;
    @Enumerated(EnumType.STRING)
    private CourseType type;
    private Long courseVersion;
    @OneToOne
    @JsonIgnore
    private CourseExecution courseExecution;

    public CourseExecutionCourse() {}

    public CourseExecutionCourse(CourseExecutionDto dto) {
        setCourseAggregateId(dto.getCourseAggregateId());
        setName(dto.getName());
        setType(dto.getType() != null ? CourseType.valueOf(dto.getType()) : null);
        setCourseVersion(dto.getCourseVersion());
    }

    public CourseExecutionCourse(CourseExecutionCourse other) {
        setCourseAggregateId(other.getCourseAggregateId());
        setName(other.getName());
        setType(other.getType());
        setCourseVersion(other.getCourseVersion());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCourseAggregateId() { return courseAggregateId; }
    public void setCourseAggregateId(Integer courseAggregateId) { this.courseAggregateId = courseAggregateId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public CourseType getType() { return type; }
    public void setType(CourseType type) { this.type = type; }

    public Long getCourseVersion() { return courseVersion; }
    public void setCourseVersion(Long courseVersion) { this.courseVersion = courseVersion; }

    @JsonIgnore
    public CourseExecution getCourseExecution() { return courseExecution; }
    public void setCourseExecution(CourseExecution courseExecution) { this.courseExecution = courseExecution; }
}
