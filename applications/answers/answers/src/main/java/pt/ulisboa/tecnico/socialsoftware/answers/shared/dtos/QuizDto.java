package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    private String description;
    private String quizType;
    private Integer numberOfQuestions;
    private List<QuestionDto> questions;
    
    public QuizDto() {
    }
    
    public QuizDto(Quiz quiz) {
        setAggregateId(quiz.getAggregateId());
        setVersion(quiz.getVersion());
        setState(quiz.getState());
        setTitle(quiz.getTitle());
        setAvailableDate(quiz.getAvailableDate());
        setConclusionDate(quiz.getConclusionDate());
        setResultsDate(quiz.getResultsDate());
        setDescription(quiz.getDescription());
        setQuizType(quiz.getQuizExecution().getQuizType().toString());
        setNumberOfQuestions(quiz.getNumberOfQuestions().stream().map(QuizNumberOfQuestion::buildDto).collect(Collectors.toSet()));
        setQuestions(quiz.getQuestions().stream().map(QuizQuestion::buildDto).collect(Collectors.toList()));
    }
    
    public Integer getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }
    
    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }
    
    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }
    
    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public LocalDateTime getResultsDate() {
        return resultsDate;
    }
    
    public void setResultsDate(LocalDateTime resultsDate) {
        this.resultsDate = resultsDate;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuizType() {
        return quizType;
    }
    
    public void setQuizType(String quizType) {
        this.quizType = quizType;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }
    
    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }
    
    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
}