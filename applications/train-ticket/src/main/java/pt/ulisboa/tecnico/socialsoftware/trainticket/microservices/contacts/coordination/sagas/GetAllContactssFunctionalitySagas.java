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
import java.util.List;

public class GetAllContactssFunctionalitySagas extends WorkflowFunctionality {
    private List<ContactsDto> contactss;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllContactssFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllContactssStep = new SagaStep("getAllContactssStep", () -> {
            GetAllContactssCommand cmd = new GetAllContactssCommand(unitOfWork, ServiceMapping.CONTACTS.getServiceName());
            List<ContactsDto> contactss = (List<ContactsDto>) commandGateway.send(cmd);
            setContactss(contactss);
        });

        workflow.addStep(getAllContactssStep);
    }
    public List<ContactsDto> getContactss() {
        return contactss;
    }

    public void setContactss(List<ContactsDto> contactss) {
        this.contactss = contactss;
    }
}
