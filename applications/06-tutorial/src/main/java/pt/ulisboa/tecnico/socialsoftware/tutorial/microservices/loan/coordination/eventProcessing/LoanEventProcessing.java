package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.MemberDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.BookDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class LoanEventProcessing {
    @Autowired
    private LoanService loanService;

    @Autowired
    private LoanFactory loanFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public LoanEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processMemberDeletedEvent(Integer aggregateId, MemberDeletedEvent memberDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Loan oldLoan = (Loan) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Loan newLoan = loanFactory.createLoanFromExisting(oldLoan);
        newLoan.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newLoan, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processBookDeletedEvent(Integer aggregateId, BookDeletedEvent bookDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Loan oldLoan = (Loan) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Loan newLoan = loanFactory.createLoanFromExisting(oldLoan);
        newLoan.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newLoan, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}