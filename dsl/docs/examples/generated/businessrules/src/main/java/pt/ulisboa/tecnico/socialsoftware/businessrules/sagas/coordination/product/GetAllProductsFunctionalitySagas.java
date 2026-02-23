package pt.ulisboa.tecnico.socialsoftware.businessrules.sagas.coordination.product;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllProductsFunctionalitySagas extends WorkflowFunctionality {
    private List<ProductDto> products;
    private final ProductService productService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllProductsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ProductService productService) {
        this.productService = productService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllProductsStep = new SagaSyncStep("getAllProductsStep", () -> {
            List<ProductDto> products = productService.getAllProducts(unitOfWork);
            setProducts(products);
        });

        workflow.addStep(getAllProductsStep);
    }
    public List<ProductDto> getProducts() {
        return products;
    }

    public void setProducts(List<ProductDto> products) {
        this.products = products;
    }
}
