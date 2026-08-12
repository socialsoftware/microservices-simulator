package pt.ulisboa.tecnico.socialsoftware.answers.events;

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