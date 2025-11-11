package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Integer id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    
    public TournamentDto() {
    }
    
    public TournamentDto(Integer aggregateId, Integer version, AggregateState state, Integer id, LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setId(id);
        setStartTime(startTime);
        setEndTime(endTime);
        setNumberOfQuestions(numberOfQuestions);
        setCancelled(cancelled);
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

    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }
    
    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Boolean getCancelled() {
        return cancelled;
    }
    
    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }
}