package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class QuizDto implements Serializable {
    
    // Standard aggregate fields
    private Integer aggregateId;
    private Integer version;
    private String state;

    // Root entity fields
    private String title;
    private String description;

    // Fields from QuizDetails
    private Integer id;
    private String quizType;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private Integer numberOfQuestions;
    
    public QuizDto() {
    }
    
    public QuizDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz quiz) {
        // Standard aggregate fields
        setAggregateId(quiz.getAggregateId());
        setVersion(quiz.getVersion());
        setState(quiz.getState().toString());

        // Root entity fields
        setTitle(quiz.getTitle());
        setDescription(quiz.getDescription());

        // Fields from QuizDetails
        setId(quiz.getQuizDetails().getId());
        setQuizType(quiz.getQuizDetails().getQuizType().toString());
        setAvailableDate(quiz.getQuizDetails().getAvailableDate());
        setConclusionDate(quiz.getQuizDetails().getConclusionDate());
        setNumberOfQuestions(quiz.getQuizDetails().getNumberOfQuestions());

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

    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }

    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
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

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }
    
    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }
}