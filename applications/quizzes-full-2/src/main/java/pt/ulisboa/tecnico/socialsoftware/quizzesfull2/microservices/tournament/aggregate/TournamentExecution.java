package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_executions")
public class TournamentExecution {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer executionAggregateId;
    private Long executionVersion;
    private Integer courseAggregateId;
    @OneToOne
    private Tournament tournament;

    public TournamentExecution() {
    }

    public TournamentExecution(Integer executionAggregateId, Long executionVersion, Integer courseAggregateId) {
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
        this.courseAggregateId = courseAggregateId;
    }

    public TournamentExecution(TournamentExecution other) {
        this.executionAggregateId = other.getExecutionAggregateId();
        this.executionVersion = other.getExecutionVersion();
        this.courseAggregateId = other.getCourseAggregateId();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Long getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Long executionVersion) {
        this.executionVersion = executionVersion;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
}
