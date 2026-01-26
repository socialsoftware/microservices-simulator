package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentDeletedEvent extends Event {

    public TournamentDeletedEvent() {
        super();
    }

    public TournamentDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}