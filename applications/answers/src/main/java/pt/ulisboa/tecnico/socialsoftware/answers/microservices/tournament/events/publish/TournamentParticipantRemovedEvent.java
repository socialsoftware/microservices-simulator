package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentParticipantRemovedEvent extends Event {
    private Integer participantAggregateId;

    public TournamentParticipantRemovedEvent() {
        super();
    }

    public TournamentParticipantRemovedEvent(Integer aggregateId, Integer participantAggregateId) {
        super(aggregateId);
        setParticipantAggregateId(participantAggregateId);
    }

    public Integer getParticipantAggregateId() {
        return participantAggregateId;
    }

    public void setParticipantAggregateId(Integer participantAggregateId) {
        this.participantAggregateId = participantAggregateId;
    }

}