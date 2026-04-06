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

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe.LoanSubscribesBookDeletedBookExists;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe.LoanSubscribesMemberDeleted;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe.LoanSubscribesMemberDeletedMemberExists;

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
        setMember(new LoanMember(other.getMember()));
        setBook(new LoanBook(other.getBook()));
        setLoanDate(other.getLoanDate());
        setDueDate(other.getDueDate());
    }

    public LoanMember getMember() {
        return member;
    }

    public void setMember(LoanMember member) {
        this.member = member;
    }

    public LoanBook getBook() {
        return book;
    }

    public void setBook(LoanBook book) {
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantMemberExists(eventSubscriptions);
            interInvariantBookExists(eventSubscriptions);
            eventSubscriptions.add(new LoanSubscribesMemberDeleted(this));
        }
        return eventSubscriptions;
    }
    private void interInvariantMemberExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new LoanSubscribesMemberDeletedMemberExists(this.getMember()));
    }

    private void interInvariantBookExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new LoanSubscribesBookDeletedBookExists(this.getBook()));
    }


    private boolean invariantMemberNotNull() {
        return this.member != null;
    }

    private boolean invariantBookNotNull() {
        return this.book != null;
    }

    private boolean invariantDateOrdering() {
        return this.loanDate.isBefore(this.dueDate);
    }
    @Override
    public void verifyInvariants() {
        if (!invariantMemberNotNull()) {
            throw new SimulatorException(INVARIANT_BREAK, "Loan must have a member");
        }
        if (!invariantBookNotNull()) {
            throw new SimulatorException(INVARIANT_BREAK, "Loan must have a book");
        }
        if (!invariantDateOrdering()) {
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