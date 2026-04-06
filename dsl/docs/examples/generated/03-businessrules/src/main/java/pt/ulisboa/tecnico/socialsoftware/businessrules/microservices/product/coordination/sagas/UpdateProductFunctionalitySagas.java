package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.businessrules.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.businessrules.command.product.*;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateProductFunctionalitySagas extends WorkflowFunctionality {
    private ProductDto updatedProductDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateProductFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, ProductDto productDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(productDto, unitOfWork);
    }

    public void buildWorkflow(ProductDto productDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateProductStep = new SagaStep("updateProductStep", () -> {
            UpdateProductCommand cmd = new UpdateProductCommand(unitOfWork, ServiceMapping.PRODUCT.getServiceName(), productDto);
            ProductDto updatedProductDto = (ProductDto) commandGateway.send(cmd);
            setUpdatedProductDto(updatedProductDto);
        });

        workflow.addStep(updateProductStep);
    }
    public ProductDto getUpdatedProductDto() {
        return updatedProductDto;
    }

    public void setUpdatedProductDto(ProductDto updatedProductDto) {
        this.updatedProductDto = updatedProductDto;
    }
}
