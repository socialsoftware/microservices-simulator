package pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.coordination.contact;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateContactFunctionalitySagas extends WorkflowFunctionality {
    private ContactDto updatedContactDto;
    private final ContactService contactService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateContactFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ContactService contactService, ContactDto contactDto) {
        this.contactService = contactService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(contactDto, unitOfWork);
    }

    public void buildWorkflow(ContactDto contactDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateContactStep = new SagaSyncStep("updateContactStep", () -> {
            ContactDto updatedContactDto = contactService.updateContact(contactDto, unitOfWork);
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
