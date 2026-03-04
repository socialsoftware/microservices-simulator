package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.product.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.coordination.webapi.requestDtos.CreateProductRequestDto;

public class CreateProductFunctionalitySagas extends WorkflowFunctionality {
    private ProductDto createdProductDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateProductFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateProductRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateProductRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createProductStep = new SagaStep("createProductStep", () -> {
            CreateProductCommand cmd = new CreateProductCommand(unitOfWork, ServiceMapping.PRODUCT.getServiceName(), createRequest);
            ProductDto createdProductDto = (ProductDto) commandGateway.send(cmd);
            setCreatedProductDto(createdProductDto);
        });

        workflow.addStep(createProductStep);
    }
    public ProductDto getCreatedProductDto() {
        return createdProductDto;
    }

    public void setCreatedProductDto(ProductDto createdProductDto) {
        this.createdProductDto = createdProductDto;
    }
}
