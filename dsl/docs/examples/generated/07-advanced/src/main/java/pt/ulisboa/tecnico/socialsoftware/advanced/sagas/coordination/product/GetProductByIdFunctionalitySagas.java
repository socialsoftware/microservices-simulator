package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetProductByIdFunctionalitySagas extends WorkflowFunctionality {
    private ProductDto productDto;
    private final ProductService productService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetProductByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ProductService productService, Integer productAggregateId) {
        this.productService = productService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(productAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer productAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getProductStep = new SagaSyncStep("getProductStep", () -> {
            ProductDto productDto = productService.getProductById(productAggregateId, unitOfWork);
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
