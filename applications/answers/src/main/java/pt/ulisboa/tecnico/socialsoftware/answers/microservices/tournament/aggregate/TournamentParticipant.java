package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Embeddable
public class TournamentParticipant {
    private Long id;
    private Integer participantAggregateId;
    private String participantName;
    private String participantUsername;
    private LocalDateTime enrollTime;
    private Object participantAnswer;
    private Integer participantVersion;
    private AggregateState state;
    private Object tournament; 

    public TournamentParticipant(Long id, Integer participantAggregateId, String participantName, String participantUsername, LocalDateTime enrollTime, Object participantAnswer, Integer participantVersion, AggregateState state, Object tournament) {
        this.id = id;
        this.participantAggregateId = participantAggregateId;
        this.participantName = participantName;
        this.participantUsername = participantUsername;
        this.enrollTime = enrollTime;
        this.participantAnswer = participantAnswer;
        this.participantVersion = participantVersion;
        this.state = state;
        this.tournament = tournament;
    }

    public TournamentParticipant(TournamentParticipant other) {
        // Copy constructor
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getParticipantAggregateId() {
        return participantAggregateId;
    }

    public void setParticipantAggregateId(Integer participantAggregateId) {
        this.participantAggregateId = participantAggregateId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantUsername() {
        return participantUsername;
    }

    public void setParticipantUsername(String participantUsername) {
        this.participantUsername = participantUsername;
    }

    public LocalDateTime getEnrollTime() {
        return enrollTime;
    }

    public void setEnrollTime(LocalDateTime enrollTime) {
        this.enrollTime = enrollTime;
    }

    public Object getParticipantAnswer() {
        return participantAnswer;
    }

    public void setParticipantAnswer(Object participantAnswer) {
        this.participantAnswer = participantAnswer;
    }

    public Integer getParticipantVersion() {
        return participantVersion;
    }

    public void setParticipantVersion(Integer participantVersion) {
        this.participantVersion = participantVersion;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Object getTournament() {
        return tournament;
    }

    public void setTournament(Object tournament) {
        this.tournament = tournament;
    }


}