package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.contacts.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class UpdateContactsFunctionalitySagas extends WorkflowFunctionality {
    private ContactsDto updatedContactsDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateContactsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, ContactsDto contactsDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(contactsDto, unitOfWork);
    }

    public void buildWorkflow(ContactsDto contactsDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateContactsStep = new SagaStep("updateContactsStep", () -> {
            UpdateContactsCommand cmd = new UpdateContactsCommand(unitOfWork, ServiceMapping.CONTACTS.getServiceName(), contactsDto);
            ContactsDto updatedContactsDto = (ContactsDto) commandGateway.send(cmd);
            setUpdatedContactsDto(updatedContactsDto);
        });

        workflow.addStep(updateContactsStep);
    }
    public ContactsDto getUpdatedContactsDto() {
        return updatedContactsDto;
    }

    public void setUpdatedContactsDto(ContactsDto updatedContactsDto) {
        this.updatedContactsDto = updatedContactsDto;
    }
}
