package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.GetContactsByAccountFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.GetContactsByIdFunctionalitySagas;

import java.util.List;

@Service
public class ContactsFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public ContactsDto getContactsById(Integer contactsAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getContactsById");
        GetContactsByIdFunctionalitySagas saga = new GetContactsByIdFunctionalitySagas(
                unitOfWorkService, contactsAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getContactsDto();
    }

    public List<ContactsDto> getContactsByAccount(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getContactsByAccount");
        GetContactsByAccountFunctionalitySagas saga = new GetContactsByAccountFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getContacts();
    }
}
