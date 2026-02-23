package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class MemberDeletedEvent extends Event {

    public MemberDeletedEvent() {
        super();
    }

    public MemberDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}