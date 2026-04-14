package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService;

@Service
public class ContactEventProcessing {
    @Autowired
    private ContactService contactService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ContactEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}