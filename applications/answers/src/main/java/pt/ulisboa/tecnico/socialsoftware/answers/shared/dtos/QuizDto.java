package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Integer id;
    private String title;
    private String description;
    private String quizType;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private Integer numberOfQuestions;
    
    public QuizDto() {
    }
    
    public QuizDto(Integer aggregateId, Integer version, AggregateState state, Integer id, String title, String description, String quizType, LocalDateTime availableDate, LocalDateTime conclusionDate, Integer numberOfQuestions) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setId(id);
        setTitle(title);
        setDescription(description);
        setQuizType(quizType);
        setAvailableDate(availableDate);
        setConclusionDate(conclusionDate);
        setNumberOfQuestions(numberOfQuestions);
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