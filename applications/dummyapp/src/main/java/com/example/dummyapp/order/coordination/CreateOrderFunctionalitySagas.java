package com.example.dummyapp.order.coordination;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateOrderFunctionalitySagas extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         SagaUnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep createOrderStep = new SagaStep("createOrderStep", () -> {});
        workflow.addStep(createOrderStep);
    }
}
