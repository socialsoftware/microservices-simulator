package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.exception.TypesenumsErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.exception.TypesenumsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi.requestDtos.CreateContactRequestDto;
import java.util.List;

@Service
public class ContactFunctionalities {
    @Autowired
    private ContactService contactService;

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
            throw new TypesenumsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ContactDto createContact(CreateContactRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateContactFunctionalitySagas createContactFunctionalitySagas = new CreateContactFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createContactFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createContactFunctionalitySagas.getCreatedContactDto();
            default: throw new TypesenumsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ContactDto getContactById(Integer contactAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetContactByIdFunctionalitySagas getContactByIdFunctionalitySagas = new GetContactByIdFunctionalitySagas(
                        sagaUnitOfWorkService, contactAggregateId, sagaUnitOfWork, commandGateway);
                getContactByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getContactByIdFunctionalitySagas.getContactDto();
            default: throw new TypesenumsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ContactDto updateContact(ContactDto contactDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(contactDto);
                UpdateContactFunctionalitySagas updateContactFunctionalitySagas = new UpdateContactFunctionalitySagas(
                        sagaUnitOfWorkService, contactDto, sagaUnitOfWork, commandGateway);
                updateContactFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateContactFunctionalitySagas.getUpdatedContactDto();
            default: throw new TypesenumsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteContact(Integer contactAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteContactFunctionalitySagas deleteContactFunctionalitySagas = new DeleteContactFunctionalitySagas(
                        sagaUnitOfWorkService, contactAggregateId, sagaUnitOfWork, commandGateway);
                deleteContactFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TypesenumsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ContactDto> getAllContacts() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllContactsFunctionalitySagas getAllContactsFunctionalitySagas = new GetAllContactsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllContactsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllContactsFunctionalitySagas.getContacts();
            default: throw new TypesenumsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(ContactDto contactDto) {
        if (contactDto.getFirstName() == null) {
            throw new TypesenumsException(CONTACT_MISSING_FIRSTNAME);
        }
        if (contactDto.getLastName() == null) {
            throw new TypesenumsException(CONTACT_MISSING_LASTNAME);
        }
        if (contactDto.getEmail() == null) {
            throw new TypesenumsException(CONTACT_MISSING_EMAIL);
        }
}

    private void checkInput(CreateContactRequestDto createRequest) {
        if (createRequest.getFirstName() == null) {
            throw new TypesenumsException(CONTACT_MISSING_FIRSTNAME);
        }
        if (createRequest.getLastName() == null) {
            throw new TypesenumsException(CONTACT_MISSING_LASTNAME);
        }
        if (createRequest.getEmail() == null) {
            throw new TypesenumsException(CONTACT_MISSING_EMAIL);
        }
}
}