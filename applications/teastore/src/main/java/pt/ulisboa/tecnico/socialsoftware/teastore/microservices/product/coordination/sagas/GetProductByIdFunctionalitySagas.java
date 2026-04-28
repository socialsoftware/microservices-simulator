package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.teastore.command.product.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.states.ProductSagaState;

public class GetProductByIdFunctionalitySagas extends WorkflowFunctionality {
    private ProductDto productDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetProductByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer productAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(productAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer productAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getProductStep = new SagaStep("getProductStep", () -> {
            unitOfWorkService.verifySagaState(productAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(ProductSagaState.UPDATE_PRODUCT, ProductSagaState.DELETE_PRODUCT)));
            unitOfWorkService.registerSagaState(productAggregateId, ProductSagaState.READ_PRODUCT, unitOfWork);
            GetProductByIdCommand cmd = new GetProductByIdCommand(unitOfWork, ServiceMapping.PRODUCT.getServiceName(), productAggregateId);
            ProductDto productDto = (ProductDto) commandGateway.send(cmd);
            setProductDto(productDto);
        });

        workflow.addStep(getProductStep);
    }
    public ProductDto getProductDto() {
        return productDto;
    }

    public void setProductDto(ProductDto productDto) {
        this.productDto = productDto;
    }
}
