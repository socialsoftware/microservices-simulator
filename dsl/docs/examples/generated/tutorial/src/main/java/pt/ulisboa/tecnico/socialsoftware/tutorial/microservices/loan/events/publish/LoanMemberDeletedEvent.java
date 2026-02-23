package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class LoanMemberDeletedEvent extends Event {
    private Integer memberAggregateId;

    public LoanMemberDeletedEvent() {
        super();
    }

    public LoanMemberDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public LoanMemberDeletedEvent(Integer aggregateId, Integer memberAggregateId) {
        super(aggregateId);
        setMemberAggregateId(memberAggregateId);
    }

    public Integer getMemberAggregateId() {
        return memberAggregateId;
    }

    public void setMemberAggregateId(Integer memberAggregateId) {
        this.memberAggregateId = memberAggregateId;
    }

}