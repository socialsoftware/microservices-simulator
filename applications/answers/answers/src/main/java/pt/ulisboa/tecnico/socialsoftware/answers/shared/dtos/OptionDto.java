package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class OptionDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Integer key;
    private Integer sequence;
    private Boolean correct;
    private String content;
    
    public OptionDto() {
    }
    
    public OptionDto(Integer aggregateId, Integer version, AggregateState state, Integer key, Integer sequence, Boolean correct, String content) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setKey(key);
        setSequence(sequence);
        setCorrect(correct);
        setContent(content);
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

    public Integer getKey() {
        return key;
    }
    
    public void setKey(Integer key) {
        this.key = key;
    }

    public Integer getSequence() {
        return sequence;
    }
    
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Boolean getCorrect() {
        return correct;
    }
    
    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}