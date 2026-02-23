package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetOrderProductFunctionalitySagas extends WorkflowFunctionality {
    private OrderProductDto productDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetOrderProductFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, Integer productAggregateId) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, productAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer productAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getProductStep = new SagaSyncStep("getProductStep", () -> {
            OrderProductDto productDto = orderService.getOrderProduct(orderId, productAggregateId, unitOfWork);
            setProductDto(productDto);
        });

        workflow.addStep(getProductStep);
    }
    public OrderProductDto getProductDto() {
        return productDto;
    }

    public void setProductDto(OrderProductDto productDto) {
        this.productDto = productDto;
    }
}
