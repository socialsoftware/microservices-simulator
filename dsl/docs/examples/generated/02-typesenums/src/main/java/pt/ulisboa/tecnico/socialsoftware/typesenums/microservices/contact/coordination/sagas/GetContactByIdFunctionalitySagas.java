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

public class GetContactByIdFunctionalitySagas extends WorkflowFunctionality {
    private ContactDto contactDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetContactByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer contactAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(contactAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer contactAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getContactStep = new SagaStep("getContactStep", () -> {
            GetContactByIdCommand cmd = new GetContactByIdCommand(unitOfWork, ServiceMapping.CONTACT.getServiceName(), contactAggregateId);
            ContactDto contactDto = (ContactDto) commandGateway.send(cmd);
            setContactDto(contactDto);
        });

        workflow.addStep(getContactStep);
    }
    public ContactDto getContactDto() {
        return contactDto;
    }

    public void setContactDto(ContactDto contactDto) {
        this.contactDto = contactDto;
    }
}
