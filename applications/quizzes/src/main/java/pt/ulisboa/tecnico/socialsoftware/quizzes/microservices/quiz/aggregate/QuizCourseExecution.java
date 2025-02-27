package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;

@Entity
public class QuizCourseExecution {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseExecutionAggregateId;
    private Integer courseExecutionVersion;
    @OneToOne
    private Quiz quiz;

    public QuizCourseExecution() {

    }

    public QuizCourseExecution(CourseExecutionDto courseExecutionDto) {
        setCourseExecutionAggregateId(courseExecutionDto.getAggregateId());
        setCourseExecutionVersion(courseExecutionDto.getVersion());
    }

    public QuizCourseExecution(QuizCourseExecution other) {
        setCourseExecutionAggregateId(other.getCourseExecutionAggregateId());
        setCourseExecutionVersion(other.getCourseExecutionVersion());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public void setCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }

    public Integer getCourseExecutionVersion() {
        return courseExecutionVersion;
    }

    public void setCourseExecutionVersion(Integer courseExecutionVersion) {
        this.courseExecutionVersion = courseExecutionVersion;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }
}
