package pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.coordination.contact;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetContactByIdFunctionalitySagas extends WorkflowFunctionality {
    private ContactDto contactDto;
    private final ContactService contactService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetContactByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ContactService contactService, Integer contactAggregateId) {
        this.contactService = contactService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(contactAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer contactAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getContactStep = new SagaSyncStep("getContactStep", () -> {
            ContactDto contactDto = contactService.getContactById(contactAggregateId, unitOfWork);
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
