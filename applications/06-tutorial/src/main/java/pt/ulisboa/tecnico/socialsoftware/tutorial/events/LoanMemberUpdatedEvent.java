package pt.ulisboa.tecnico.socialsoftware.tutorial.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class LoanMemberUpdatedEvent extends Event {
    @Column(name = "loan_member_updated_event_member_aggregate_id")
    private Integer memberAggregateId;
    @Column(name = "loan_member_updated_event_member_version")
    private Integer memberVersion;
    @Column(name = "loan_member_updated_event_member_name")
    private String memberName;
    @Column(name = "loan_member_updated_event_member_email")
    private String memberEmail;

    public LoanMemberUpdatedEvent() {
        super();
    }

    public LoanMemberUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public LoanMemberUpdatedEvent(Integer aggregateId, Integer memberAggregateId, Integer memberVersion, String memberName, String memberEmail) {
        super(aggregateId);
        setMemberAggregateId(memberAggregateId);
        setMemberVersion(memberVersion);
        setMemberName(memberName);
        setMemberEmail(memberEmail);
    }

    public Integer getMemberAggregateId() {
        return memberAggregateId;
    }

    public void setMemberAggregateId(Integer memberAggregateId) {
        this.memberAggregateId = memberAggregateId;
    }

    public Integer getMemberVersion() {
        return memberVersion;
    }

    public void setMemberVersion(Integer memberVersion) {
        this.memberVersion = memberVersion;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

}