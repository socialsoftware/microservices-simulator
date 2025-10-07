package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuestionDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Integer id;
    private String title;
    private String content;
    private Integer numberOfOptions;
    private Integer correctOption;
    private Integer order;
    
    public QuestionDto() {
    }
    
    public QuestionDto(Integer aggregateId, Integer version, AggregateState state, Integer id, String title, String content, Integer numberOfOptions, Integer correctOption, Integer order) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setId(id);
        setTitle(title);
        setContent(content);
        setNumberOfOptions(numberOfOptions);
        setCorrectOption(correctOption);
        setOrder(order);
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