package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.eventProcessing.LoanEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanRepository;

public abstract class LoanEventHandler extends EventHandler {
    private LoanRepository loanRepository;
    protected LoanEventProcessing loanEventProcessing;

    public LoanEventHandler(LoanRepository loanRepository, LoanEventProcessing loanEventProcessing) {
        this.loanRepository = loanRepository;
        this.loanEventProcessing = loanEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return loanRepository.findAll().stream().map(Loan::getAggregateId).collect(Collectors.toSet());
    }

}
