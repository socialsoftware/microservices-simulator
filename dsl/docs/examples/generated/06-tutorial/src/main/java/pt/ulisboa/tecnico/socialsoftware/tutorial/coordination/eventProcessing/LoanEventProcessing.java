package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.events.publish.MemberDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.events.publish.BookDeletedEvent;

@Service
public class LoanEventProcessing {
    @Autowired
    private LoanService loanService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public LoanEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processMemberDeletedEvent(Integer aggregateId, MemberDeletedEvent memberDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processBookDeletedEvent(Integer aggregateId, BookDeletedEvent bookDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}