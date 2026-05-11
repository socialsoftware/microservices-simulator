package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuizDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private String title;
    private LocalDateTime creationDate;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    private String quizType;
    private Integer executionId;
    private Long executionVersion;
    private List<Integer> questionIds = new ArrayList<>();

    public QuizDto() {}

    public QuizDto(Quiz quiz) {
        setAggregateId(quiz.getAggregateId());
        setVersion(quiz.getVersion());
        setState(quiz.getState());
        setTitle(quiz.getTitle());
        setCreationDate(quiz.getCreationDate());
        setAvailableDate(quiz.getAvailableDate());
        setConclusionDate(quiz.getConclusionDate());
        setResultsDate(quiz.getResultsDate());
        if (quiz.getQuizType() != null) {
            setQuizType(quiz.getQuizType().name());
        }
        if (quiz.getQuizExecution() != null) {
            setExecutionId(quiz.getQuizExecution().getExecutionAggregateId());
            setExecutionVersion(quiz.getQuizExecution().getExecutionVersion());
        }
        for (QuizQuestion q : quiz.getQuestions()) {
            this.questionIds.add(q.getQuestionAggregateId());
        }
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public AggregateState getState() { return state; }
    public void setState(AggregateState state) { this.state = state; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    public LocalDateTime getAvailableDate() { return availableDate; }
    public void setAvailableDate(LocalDateTime availableDate) { this.availableDate = availableDate; }

    public LocalDateTime getConclusionDate() { return conclusionDate; }
    public void setConclusionDate(LocalDateTime conclusionDate) { this.conclusionDate = conclusionDate; }

    public LocalDateTime getResultsDate() { return resultsDate; }
    public void setResultsDate(LocalDateTime resultsDate) { this.resultsDate = resultsDate; }

    public String getQuizType() { return quizType; }
    public void setQuizType(String quizType) { this.quizType = quizType; }

    public Integer getExecutionId() { return executionId; }
    public void setExecutionId(Integer executionId) { this.executionId = executionId; }

    public Long getExecutionVersion() { return executionVersion; }
    public void setExecutionVersion(Long executionVersion) { this.executionVersion = executionVersion; }

    public List<Integer> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Integer> questionIds) { this.questionIds = questionIds; }
}
