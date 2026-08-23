package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.UpdateContactsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.states.ContactsSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateContactsFunctionalitySagas extends WorkflowFunctionality {
    private ContactsDto contactsDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateContactsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer contactsAggregateId, ContactsDto updatedContactsDto,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(contactsAggregateId, updatedContactsDto, unitOfWork);
    }

    public void buildWorkflow(Integer contactsAggregateId, ContactsDto updatedContactsDto,
                              SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getContactsStep = new SagaStep("getContactsStep", () -> {
            GetContactsByIdCommand readCmd = new GetContactsByIdCommand(
                    unitOfWork, ServiceMapping.CONTACTS.getServiceName(), contactsAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(ContactsSagaState.IN_UPDATE_CONTACTS);
            this.contactsDto = (ContactsDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateContactsStep = new SagaStep("updateContactsStep", () -> {
            UpdateContactsCommand cmd = new UpdateContactsCommand(
                    unitOfWork, ServiceMapping.CONTACTS.getServiceName(),
                    contactsAggregateId, updatedContactsDto);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getContactsStep)));

        this.workflow.addStep(getContactsStep);
        this.workflow.addStep(updateContactsStep);
    }

    public ContactsDto getContactsDto() {
        return contactsDto;
    }
}
