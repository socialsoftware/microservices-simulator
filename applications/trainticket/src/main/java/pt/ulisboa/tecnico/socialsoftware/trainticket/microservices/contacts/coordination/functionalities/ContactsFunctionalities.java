package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.CreateContactsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.DeleteContactsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.GetContactsByAccountFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.GetContactsByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.UpdateContactsFunctionalitySagas;

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

    public ContactsDto createContacts(ContactsDto contactsDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createContacts");
        CreateContactsFunctionalitySagas saga = new CreateContactsFunctionalitySagas(
                unitOfWorkService, contactsDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedContactsDto();
    }

    public void updateContacts(Integer contactsAggregateId, ContactsDto contactsDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateContacts");
        UpdateContactsFunctionalitySagas saga = new UpdateContactsFunctionalitySagas(
                unitOfWorkService, contactsAggregateId, contactsDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteContacts(Integer contactsAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deleteContacts");
        DeleteContactsFunctionalitySagas saga = new DeleteContactsFunctionalitySagas(
                unitOfWorkService, contactsAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
