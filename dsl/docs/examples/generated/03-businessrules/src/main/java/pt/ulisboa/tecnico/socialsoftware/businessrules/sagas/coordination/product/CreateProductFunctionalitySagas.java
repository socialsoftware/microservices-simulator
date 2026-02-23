package pt.ulisboa.tecnico.socialsoftware.businessrules.sagas.coordination.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.businessrules.coordination.webapi.requestDtos.CreateProductRequestDto;

public class CreateProductFunctionalitySagas extends WorkflowFunctionality {
    private ProductDto createdProductDto;
    private final ProductService productService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateProductFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ProductService productService, CreateProductRequestDto createRequest) {
        this.productService = productService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateProductRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createProductStep = new SagaSyncStep("createProductStep", () -> {
            ProductDto createdProductDto = productService.createProduct(createRequest, unitOfWork);
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
