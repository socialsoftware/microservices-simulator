package com.example.dummyapp.order.coordination;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaWorkflow;

import java.util.Set;

public class CreateOrderFunctionalitySagas extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;
    private final Integer customerId;
    private final Integer projectedCustomerId;

    public CreateOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         SagaUnitOfWork unitOfWork) {
        this(unitOfWorkService, unitOfWork, null, null);
    }

    public CreateOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         SagaUnitOfWork unitOfWork,
                                         Set<Integer> excludedCustomerIds,
                                         Integer customerId,
                                         Integer projectedCustomerId) {
        this(unitOfWorkService, unitOfWork, customerId, projectedCustomerId);
    }

    public CreateOrderFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          SagaUnitOfWork unitOfWork,
                                          Integer customerId,
                                          Integer projectedCustomerId) {
        this.unitOfWorkService = unitOfWorkService;
        this.customerId = customerId;
        this.projectedCustomerId = projectedCustomerId;
        buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep createOrderStep = new SagaStep("createOrderStep", () -> {});
        workflow.addStep(createOrderStep);
    }
}
