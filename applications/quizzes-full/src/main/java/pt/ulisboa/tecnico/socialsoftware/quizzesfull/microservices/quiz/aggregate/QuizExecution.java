package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;

@Entity
public class QuizExecution {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer executionAggregateId;
    private Long executionVersion;
    @OneToOne
    private Quiz quiz;

    public QuizExecution() {}

    public QuizExecution(ExecutionDto executionDto) {
        this.executionAggregateId = executionDto.getAggregateId();
        this.executionVersion = executionDto.getVersion();
    }

    public QuizExecution(QuizExecution other) {
        this.executionAggregateId = other.getExecutionAggregateId();
        this.executionVersion = other.getExecutionVersion();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public void setExecutionAggregateId(Integer executionAggregateId) { this.executionAggregateId = executionAggregateId; }

    public Long getExecutionVersion() { return executionVersion; }
    public void setExecutionVersion(Long executionVersion) { this.executionVersion = executionVersion; }

    @JsonIgnore
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
}
