package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service.ContactsService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.webapi.requestDtos.CreateContactsRequestDto;
import java.util.List;

@Service
public class ContactsFunctionalities {
    @Autowired
    private ContactsService contactsService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ContactsDto createContacts(CreateContactsRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateContactsFunctionalitySagas createContactsFunctionalitySagas = new CreateContactsFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createContactsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createContactsFunctionalitySagas.getCreatedContactsDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ContactsDto getContactsById(Integer contactsAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetContactsByIdFunctionalitySagas getContactsByIdFunctionalitySagas = new GetContactsByIdFunctionalitySagas(
                        sagaUnitOfWorkService, contactsAggregateId, sagaUnitOfWork, commandGateway);
                getContactsByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getContactsByIdFunctionalitySagas.getContactsDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ContactsDto updateContacts(ContactsDto contactsDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(contactsDto);
                UpdateContactsFunctionalitySagas updateContactsFunctionalitySagas = new UpdateContactsFunctionalitySagas(
                        sagaUnitOfWorkService, contactsDto, sagaUnitOfWork, commandGateway);
                updateContactsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateContactsFunctionalitySagas.getUpdatedContactsDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteContacts(Integer contactsAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteContactsFunctionalitySagas deleteContactsFunctionalitySagas = new DeleteContactsFunctionalitySagas(
                        sagaUnitOfWorkService, contactsAggregateId, sagaUnitOfWork, commandGateway);
                deleteContactsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ContactsDto> getAllContactss() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllContactssFunctionalitySagas getAllContactssFunctionalitySagas = new GetAllContactssFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllContactssFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllContactssFunctionalitySagas.getContactss();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(ContactsDto contactsDto) {
        if (contactsDto.getName() == null) {
            throw new TrainTicketException(CONTACTS_MISSING_NAME);
        }
        if (contactsDto.getDocumentNumber() == null) {
            throw new TrainTicketException(CONTACTS_MISSING_DOCUMENTNUMBER);
        }
        if (contactsDto.getPhoneNumber() == null) {
            throw new TrainTicketException(CONTACTS_MISSING_PHONENUMBER);
        }
}

    private void checkInput(CreateContactsRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new TrainTicketException(CONTACTS_MISSING_NAME);
        }
        if (createRequest.getDocumentNumber() == null) {
            throw new TrainTicketException(CONTACTS_MISSING_DOCUMENTNUMBER);
        }
        if (createRequest.getPhoneNumber() == null) {
            throw new TrainTicketException(CONTACTS_MISSING_PHONENUMBER);
        }
}
}