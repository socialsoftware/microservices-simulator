package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByAccountCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;

import java.util.List;

public class GetContactsByAccountFunctionalitySagas extends WorkflowFunctionality {
    private List<ContactsDto> contacts;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetContactsByAccountFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                  Integer userAggregateId,
                                                  SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(userAggregateId, unitOfWork);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getContactsStep = new SagaStep("getContactsStep", () -> {
            GetContactsByAccountCommand cmd = new GetContactsByAccountCommand(
                    unitOfWork, ServiceMapping.CONTACTS.getServiceName(), userAggregateId);
            this.contacts = (List<ContactsDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getContactsStep);
    }

    public List<ContactsDto> getContacts() {
        return contacts;
    }
}
