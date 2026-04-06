package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentParticipantRemovedEvent extends Event {
    @Column(name = "tournament_participant_removed_event_participant_aggregate_id")
    private Integer participantAggregateId;

    public TournamentParticipantRemovedEvent() {
        super();
    }

    public TournamentParticipantRemovedEvent(Integer aggregateId) {
        super(aggregateId);
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