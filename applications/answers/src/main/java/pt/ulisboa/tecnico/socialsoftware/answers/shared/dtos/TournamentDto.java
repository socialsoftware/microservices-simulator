package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class TournamentDto implements Serializable {
    
    // Standard aggregate fields
    private Integer aggregateId;
    private Integer version;
    private String state;

    // Root entity fields
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Fields from TournamentDetails
    private Integer id;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    
    public TournamentDto() {
    }
    
    public TournamentDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament tournament) {
        // Standard aggregate fields
        setAggregateId(tournament.getAggregateId());
        setVersion(tournament.getVersion());
        setState(tournament.getState().toString());

        // Root entity fields
        setStartTime(tournament.getStartTime());
        setEndTime(tournament.getEndTime());

        // Fields from TournamentDetails
        setId(tournament.getTournamentDetails().getId());
        setNumberOfQuestions(tournament.getTournamentDetails().getNumberOfQuestions());
        setCancelled(tournament.getTournamentDetails().getCancelled());

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