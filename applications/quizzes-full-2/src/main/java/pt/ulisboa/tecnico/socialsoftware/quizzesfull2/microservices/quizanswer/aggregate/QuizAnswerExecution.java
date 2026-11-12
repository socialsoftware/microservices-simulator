package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_answer_executions")
public class QuizAnswerExecution {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer executionAggregateId;
    private Long executionVersion;
    @OneToOne
    private QuizAnswer quizAnswer;

    public QuizAnswerExecution() {
    }

    public QuizAnswerExecution(Integer executionAggregateId, Long executionVersion) {
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
    }

    public QuizAnswerExecution(QuizAnswerExecution other) {
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

    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
