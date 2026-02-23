package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanMemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;

@Entity
public class LoanMember {
    @Id
    @GeneratedValue
    private Long id;
    private String memberName;
    private String memberEmail;
    private Integer memberAggregateId;
    private Integer memberVersion;
    private AggregateState memberState;
    @OneToOne
    private Loan loan;

    public LoanMember() {

    }

    public LoanMember(MemberDto memberDto) {
        setMemberAggregateId(memberDto.getAggregateId());
        setMemberVersion(memberDto.getVersion());
        setMemberState(memberDto.getState());
    }

    public LoanMember(LoanMemberDto loanMemberDto) {
        setMemberName(loanMemberDto.getName());
        setMemberEmail(loanMemberDto.getEmail());
        setMemberAggregateId(loanMemberDto.getAggregateId());
        setMemberVersion(loanMemberDto.getVersion());
        setMemberState(loanMemberDto.getState());
    }

    public LoanMember(LoanMember other) {
        setMemberName(other.getMemberName());
        setMemberEmail(other.getMemberEmail());
        setMemberAggregateId(other.getMemberAggregateId());
        setMemberVersion(other.getMemberVersion());
        setMemberState(other.getMemberState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AggregateState getMemberState() {
        return memberState;
    }

    public void setMemberState(AggregateState memberState) {
        this.memberState = memberState;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }




    public LoanMemberDto buildDto() {
        LoanMemberDto dto = new LoanMemberDto();
        dto.setName(getMemberName());
        dto.setEmail(getMemberEmail());
        dto.setAggregateId(getMemberAggregateId());
        dto.setVersion(getMemberVersion());
        dto.setState(getMemberState());
        return dto;
    }
}