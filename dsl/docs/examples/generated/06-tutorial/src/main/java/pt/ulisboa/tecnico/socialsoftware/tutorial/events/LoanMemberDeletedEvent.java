package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class LoanMemberDeletedEvent extends Event {
    @Column(name = "loan_member_deleted_event_member_aggregate_id")
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