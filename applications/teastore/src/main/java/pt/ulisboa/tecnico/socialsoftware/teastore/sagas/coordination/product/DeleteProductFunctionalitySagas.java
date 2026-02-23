package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteProductFunctionalitySagas extends WorkflowFunctionality {
    private final ProductService productService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteProductFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ProductService productService, Integer productAggregateId) {
        this.productService = productService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(productAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer productAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteProductStep = new SagaSyncStep("deleteProductStep", () -> {
            productService.deleteProduct(productAggregateId, unitOfWork);
        });

        workflow.addStep(deleteProductStep);
    }
}
