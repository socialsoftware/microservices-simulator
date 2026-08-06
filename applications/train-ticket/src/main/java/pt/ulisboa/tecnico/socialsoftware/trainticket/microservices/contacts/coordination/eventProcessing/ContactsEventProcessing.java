package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service.ContactsService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;

@Service
public class ContactsEventProcessing {
    @Autowired
    private ContactsService contactsService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ContactsEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}