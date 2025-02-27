package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

import jakarta.persistence.*;

@Entity
public class AnswerCourseExecution {
    @Id
    @GeneratedValue
    private Long id;
    private Integer courseExecutionAggregateId;
    private Integer courseExecutionVersion;
    @OneToOne
    private QuizAnswer quizAnswer;

    public AnswerCourseExecution() {
        this.courseExecutionAggregateId = 0;
    }

    public AnswerCourseExecution(Integer courseExecutionAggregateId, Integer courseExecutionVersion) {
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        setCourseExecutionVersion(courseExecutionVersion);
    }

    public AnswerCourseExecution(AnswerCourseExecution courseExecution) {
        this.courseExecutionAggregateId = courseExecution.getCourseExecutionAggregateId();
        setCourseExecutionVersion(courseExecution.getCourseExecutionVersion());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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

    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
