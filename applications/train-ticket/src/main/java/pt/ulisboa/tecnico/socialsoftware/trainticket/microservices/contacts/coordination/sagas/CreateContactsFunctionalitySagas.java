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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.webapi.requestDtos.CreateContactsRequestDto;

public class CreateContactsFunctionalitySagas extends WorkflowFunctionality {
    private ContactsDto createdContactsDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateContactsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateContactsRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateContactsRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createContactsStep = new SagaStep("createContactsStep", () -> {
            CreateContactsCommand cmd = new CreateContactsCommand(unitOfWork, ServiceMapping.CONTACTS.getServiceName(), createRequest);
            ContactsDto createdContactsDto = (ContactsDto) commandGateway.send(cmd);
            setCreatedContactsDto(createdContactsDto);
        });

        workflow.addStep(createContactsStep);
    }
    public ContactsDto getCreatedContactsDto() {
        return createdContactsDto;
    }

    public void setCreatedContactsDto(ContactsDto createdContactsDto) {
        this.createdContactsDto = createdContactsDto;
    }
}
