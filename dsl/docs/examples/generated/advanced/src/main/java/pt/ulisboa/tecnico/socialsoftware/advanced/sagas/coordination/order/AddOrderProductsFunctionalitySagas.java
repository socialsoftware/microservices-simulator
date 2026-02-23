package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddOrderProductsFunctionalitySagas extends WorkflowFunctionality {
    private List<OrderProductDto> addedProductDtos;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddOrderProductsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, List<OrderProductDto> productDtos) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, productDtos, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, List<OrderProductDto> productDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addProductsStep = new SagaSyncStep("addProductsStep", () -> {
            List<OrderProductDto> addedProductDtos = orderService.addOrderProducts(orderId, productDtos, unitOfWork);
            setAddedProductDtos(addedProductDtos);
        });

        workflow.addStep(addProductsStep);
    }
    public List<OrderProductDto> getAddedProductDtos() {
        return addedProductDtos;
    }

    public void setAddedProductDtos(List<OrderProductDto> addedProductDtos) {
        this.addedProductDtos = addedProductDtos;
    }
}
