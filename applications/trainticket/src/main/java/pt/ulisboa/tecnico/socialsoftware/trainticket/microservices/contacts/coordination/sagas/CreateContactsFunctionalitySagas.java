package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.CreateContactsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;

public class CreateContactsFunctionalitySagas extends WorkflowFunctionality {
    private ContactsDto createdContactsDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateContactsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            ContactsDto contactsDto,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(contactsDto, unitOfWork);
    }

    public void buildWorkflow(ContactsDto contactsDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createContactsStep = new SagaStep("createContactsStep", () -> {
            CreateContactsCommand cmd = new CreateContactsCommand(
                    unitOfWork, ServiceMapping.CONTACTS.getServiceName(), contactsDto);
            this.createdContactsDto = (ContactsDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(createContactsStep);
    }

    public ContactsDto getCreatedContactsDto() {
        return createdContactsDto;
    }
}
