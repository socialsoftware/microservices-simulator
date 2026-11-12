package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_executions")
public class QuizExecution {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer executionAggregateId;
    private Long executionVersion;
    @OneToOne
    private Quiz quiz;

    public QuizExecution() {
    }

    public QuizExecution(Integer executionAggregateId, Long executionVersion) {
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
    }

    public QuizExecution(QuizExecution other) {
        this.executionAggregateId = other.getExecutionAggregateId();
        this.executionVersion = other.getExecutionVersion();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Long getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Long executionVersion) {
        this.executionVersion = executionVersion;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }
}
