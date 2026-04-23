package pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;

public class LoanDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private LoanMemberDto member;
    private LoanBookDto book;
    private LocalDateTime loanDate;
    private LocalDateTime dueDate;

    public LoanDto() {
    }

    public LoanDto(Loan loan) {
        this.aggregateId = loan.getAggregateId();
        this.version = loan.getVersion();
        this.state = loan.getState();
        this.member = loan.getMember() != null ? new LoanMemberDto(loan.getMember()) : null;
        this.book = loan.getBook() != null ? new LoanBookDto(loan.getBook()) : null;
        this.loanDate = loan.getLoanDate();
        this.dueDate = loan.getDueDate();
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

    public LoanMemberDto getMember() {
        return member;
    }

    public void setMember(LoanMemberDto member) {
        this.member = member;
    }

    public LoanBookDto getBook() {
        return book;
    }

    public void setBook(LoanBookDto book) {
        this.book = book;
    }

    public LocalDateTime getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDateTime loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
}