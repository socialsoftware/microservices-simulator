package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class QuestionDto implements Serializable {
    
    // Standard aggregate fields
    private Integer aggregateId;
    private Integer version;
    private String state;

    // Root entity fields
    private String title;
    private String content;

    // Fields from QuestionDetails
    private Integer id;
    private Integer numberOfOptions;
    private Integer correctOption;
    private Integer order;
    
    public QuestionDto() {
    }
    
    public QuestionDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question question) {
        // Standard aggregate fields
        setAggregateId(question.getAggregateId());
        setVersion(question.getVersion());
        setState(question.getState().toString());

        // Root entity fields
        setTitle(question.getTitle());
        setContent(question.getContent());

        // Fields from QuestionDetails
        setId(question.getQuestionDetails().getId());
        setNumberOfOptions(question.getQuestionDetails().getNumberOfOptions());
        setCorrectOption(question.getQuestionDetails().getCorrectOption());
        setOrder(question.getQuestionDetails().getOrder());

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

    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    public Integer getNumberOfOptions() {
        return numberOfOptions;
    }
    
    public void setNumberOfOptions(Integer numberOfOptions) {
        this.numberOfOptions = numberOfOptions;
    }

    public Integer getCorrectOption() {
        return correctOption;
    }
    
    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
}