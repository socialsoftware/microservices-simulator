package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import jakarta.persistence.CascadeType;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

@Entity
public class TournamentExecution {
    @Id
    @GeneratedValue
    private Long id;
    private Integer executionAggregateId;
    private Integer executionCourseId;
    private String executionAcronym;
    private String executionStatus;
    private Integer executionVersion;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentexecution")
    private Tournament tournament;
    @OneToOne
    private Tournament tournament; 

    public TournamentExecution() {
    }

    public TournamentExecution(TournamentDto tournamentDto) {
        setExecutionAggregateId(tournamentDto.getExecutionAggregateId());
        setExecutionCourseId(tournamentDto.getExecutionCourseId());
        setExecutionAcronym(tournamentDto.getExecutionAcronym());
        setExecutionStatus(tournamentDto.getExecutionStatus());
        setExecutionVersion(tournamentDto.getExecutionVersion());
        setTournament(tournament);
    }

    public TournamentExecution(TournamentExecution other) {
        setExecutionAggregateId(other.getExecutionAggregateId());
        setExecutionCourseId(other.getExecutionCourseId());
        setExecutionAcronym(other.getExecutionAcronym());
        setExecutionStatus(other.getExecutionStatus());
        setExecutionVersion(other.getExecutionVersion());
        setTournament(new Tournament(other.getTournament()));
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionCourseId() {
        return executionCourseId;
    }

    public void setExecutionCourseId(Integer executionCourseId) {
        this.executionCourseId = executionCourseId;
    }

    public String getExecutionAcronym() {
        return executionAcronym;
    }

    public void setExecutionAcronym(String executionAcronym) {
        this.executionAcronym = executionAcronym;
    }

    public String getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(String executionStatus) {
        this.executionStatus = executionStatus;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Integer executionVersion) {
        this.executionVersion = executionVersion;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
        if (this.tournament != null) {
            this.tournament.setTournamentExecution(this);
        }
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


}