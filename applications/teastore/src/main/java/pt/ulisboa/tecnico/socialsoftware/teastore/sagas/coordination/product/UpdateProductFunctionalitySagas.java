package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateProductFunctionalitySagas extends WorkflowFunctionality {
    private ProductDto updatedProductDto;
    private final ProductService productService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateProductFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ProductService productService, ProductDto productDto) {
        this.productService = productService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(productDto, unitOfWork);
    }

    public void buildWorkflow(ProductDto productDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateProductStep = new SagaSyncStep("updateProductStep", () -> {
            ProductDto updatedProductDto = productService.updateProduct(productDto, unitOfWork);
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
