package pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.coordination.contact;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteContactFunctionalitySagas extends WorkflowFunctionality {
    private final ContactService contactService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteContactFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ContactService contactService, Integer contactAggregateId) {
        this.contactService = contactService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(contactAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer contactAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteContactStep = new SagaSyncStep("deleteContactStep", () -> {
            contactService.deleteContact(contactAggregateId, unitOfWork);
        });

        workflow.addStep(deleteContactStep);
    }
}
