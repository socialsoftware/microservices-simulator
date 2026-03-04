package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.typesenums.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.typesenums.command.contact.*;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi.requestDtos.CreateContactRequestDto;

public class CreateContactFunctionalitySagas extends WorkflowFunctionality {
    private ContactDto createdContactDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateContactFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateContactRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateContactRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createContactStep = new SagaStep("createContactStep", () -> {
            CreateContactCommand cmd = new CreateContactCommand(unitOfWork, ServiceMapping.CONTACT.getServiceName(), createRequest);
            ContactDto createdContactDto = (ContactDto) commandGateway.send(cmd);
            setCreatedContactDto(createdContactDto);
        });

        workflow.addStep(createContactStep);
    }
    public ContactDto getCreatedContactDto() {
        return createdContactDto;
    }

    public void setCreatedContactDto(ContactDto createdContactDto) {
        this.createdContactDto = createdContactDto;
    }
}
