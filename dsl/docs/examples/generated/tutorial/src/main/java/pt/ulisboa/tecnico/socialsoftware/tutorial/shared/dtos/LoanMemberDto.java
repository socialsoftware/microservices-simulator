package pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanMember;

public class LoanMemberDto implements Serializable {
    private String name;
    private String email;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public LoanMemberDto() {
    }

    public LoanMemberDto(LoanMember loanMember) {
        this.name = loanMember.getMemberName();
        this.email = loanMember.getMemberEmail();
        this.aggregateId = loanMember.getMemberAggregateId();
        this.version = loanMember.getMemberVersion();
        this.state = loanMember.getMemberState();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}