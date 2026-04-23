package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate;

import java.util.HashSet;
import java.util.Set;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe.LoanSubscribesBookDeletedBookRef;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe.LoanSubscribesMemberDeletedMemberRef;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanBookDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanMemberDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Loan extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "loan")
    private LoanMember member;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "loan")
    private LoanBook book;
    private LocalDateTime loanDate;
    private LocalDateTime dueDate;

    public Loan() {

    }

    public Loan(Integer aggregateId, LoanDto loanDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setLoanDate(loanDto.getLoanDate());
        setDueDate(loanDto.getDueDate());
        setMember(loanDto.getMember() != null ? new LoanMember(loanDto.getMember()) : null);
        setBook(loanDto.getBook() != null ? new LoanBook(loanDto.getBook()) : null);
    }


    public Loan(Loan other) {
        super(other);
        setMember(other.getMember() != null ? new LoanMember(other.getMember()) : null);
        setBook(other.getBook() != null ? new LoanBook(other.getBook()) : null);
        setLoanDate(other.getLoanDate());
        setDueDate(other.getDueDate());
    }

    public LoanMember getMember() {
        return member;
    }

    public void setMember(LoanMember member) {
        this.member = member;
        if (this.member != null) {
            this.member.setLoan(this);
        }
    }

    public LoanBook getBook() {
        return book;
    }

    public void setBook(LoanBook book) {
        this.book = book;
        if (this.book != null) {
            this.book.setLoan(this);
        }
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantMemberRef(eventSubscriptions);
            interInvariantBookRef(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantMemberRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new LoanSubscribesMemberDeletedMemberRef(this.getMember()));
    }

    private void interInvariantBookRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new LoanSubscribesBookDeletedBookRef(this.getBook()));
    }


    private boolean invariantRule0() {
        return this.member != null;
    }

    private boolean invariantRule1() {
        return this.book != null;
    }

    private boolean invariantRule2() {
        return this.loanDate.isBefore(this.dueDate);
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Loan must have a member");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Loan must have a book");
        }
        if (!invariantRule2()) {
            throw new SimulatorException(INVARIANT_BREAK, "Loan date must be before due date");
        }
    }

    public LoanDto buildDto() {
        LoanDto dto = new LoanDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setMember(getMember() != null ? new LoanMemberDto(getMember()) : null);
        dto.setBook(getBook() != null ? new LoanBookDto(getBook()) : null);
        dto.setLoanDate(getLoanDate());
        dto.setDueDate(getDueDate());
        return dto;
    }
}