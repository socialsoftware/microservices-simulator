package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.businessrules.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.businessrules.command.product.*;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

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
