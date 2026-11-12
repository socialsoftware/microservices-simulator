package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.DeleteContactsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.states.ContactsSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteContactsFunctionalitySagas extends WorkflowFunctionality {
    private ContactsDto contactsDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteContactsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer contactsAggregateId,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(contactsAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer contactsAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getContactsStep = new SagaStep("getContactsStep", () -> {
            GetContactsByIdCommand readCmd = new GetContactsByIdCommand(
                    unitOfWork, ServiceMapping.CONTACTS.getServiceName(), contactsAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(ContactsSagaState.IN_DELETE_CONTACTS);
            this.contactsDto = (ContactsDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteContactsStep = new SagaStep("deleteContactsStep", () -> {
            DeleteContactsCommand cmd = new DeleteContactsCommand(
                    unitOfWork, ServiceMapping.CONTACTS.getServiceName(), contactsAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getContactsStep)));

        this.workflow.addStep(getContactsStep);
        this.workflow.addStep(deleteContactsStep);
    }

    public ContactsDto getContactsDto() {
        return contactsDto;
    }
}
