package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.contacts.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetContactsByIdFunctionalitySagas extends WorkflowFunctionality {
    private ContactsDto contactsDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetContactsByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer contactsAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(contactsAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer contactsAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getContactsStep = new SagaStep("getContactsStep", () -> {
            GetContactsByIdCommand cmd = new GetContactsByIdCommand(unitOfWork, ServiceMapping.CONTACTS.getServiceName(), contactsAggregateId);
            ContactsDto contactsDto = (ContactsDto) commandGateway.send(cmd);
            setContactsDto(contactsDto);
        });

        workflow.addStep(getContactsStep);
    }
    public ContactsDto getContactsDto() {
        return contactsDto;
    }

    public void setContactsDto(ContactsDto contactsDto) {
        this.contactsDto = contactsDto;
    }
}
