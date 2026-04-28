package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.typesenums.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.typesenums.command.contact.*;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.sagas.states.ContactSagaState;

public class UpdateContactFunctionalitySagas extends WorkflowFunctionality {
    private ContactDto updatedContactDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateContactFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, ContactDto contactDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(contactDto, unitOfWork);
    }

    public void buildWorkflow(ContactDto contactDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateContactStep = new SagaStep("updateContactStep", () -> {
            unitOfWorkService.verifySagaState(contactDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(ContactSagaState.READ_CONTACT, ContactSagaState.UPDATE_CONTACT, ContactSagaState.DELETE_CONTACT)));
            unitOfWorkService.registerSagaState(contactDto.getAggregateId(), ContactSagaState.UPDATE_CONTACT, unitOfWork);
            UpdateContactCommand cmd = new UpdateContactCommand(unitOfWork, ServiceMapping.CONTACT.getServiceName(), contactDto);
            ContactDto updatedContactDto = (ContactDto) commandGateway.send(cmd);
            setUpdatedContactDto(updatedContactDto);
        });

        workflow.addStep(updateContactStep);
    }
    public ContactDto getUpdatedContactDto() {
        return updatedContactDto;
    }

    public void setUpdatedContactDto(ContactDto updatedContactDto) {
        this.updatedContactDto = updatedContactDto;
    }
}
