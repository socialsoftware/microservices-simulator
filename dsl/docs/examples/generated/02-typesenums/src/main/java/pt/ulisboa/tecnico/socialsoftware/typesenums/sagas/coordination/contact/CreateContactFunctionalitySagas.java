package pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.coordination.contact;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.typesenums.coordination.webapi.requestDtos.CreateContactRequestDto;

public class CreateContactFunctionalitySagas extends WorkflowFunctionality {
    private ContactDto createdContactDto;
    private final ContactService contactService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateContactFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ContactService contactService, CreateContactRequestDto createRequest) {
        this.contactService = contactService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateContactRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createContactStep = new SagaSyncStep("createContactStep", () -> {
            ContactDto createdContactDto = contactService.createContact(createRequest, unitOfWork);
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
