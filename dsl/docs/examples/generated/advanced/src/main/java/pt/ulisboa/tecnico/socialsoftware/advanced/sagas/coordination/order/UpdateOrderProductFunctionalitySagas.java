package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateOrderProductFunctionalitySagas extends WorkflowFunctionality {
    private OrderProductDto updatedProductDto;
    private final OrderService orderService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateOrderProductFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, OrderService orderService, Integer orderId, Integer productAggregateId, OrderProductDto productDto) {
        this.orderService = orderService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(orderId, productAggregateId, productDto, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, Integer productAggregateId, OrderProductDto productDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateProductStep = new SagaSyncStep("updateProductStep", () -> {
            OrderProductDto updatedProductDto = orderService.updateOrderProduct(orderId, productAggregateId, productDto, unitOfWork);
            setUpdatedProductDto(updatedProductDto);
        });

        workflow.addStep(updateProductStep);
    }
    public OrderProductDto getUpdatedProductDto() {
        return updatedProductDto;
    }

    public void setUpdatedProductDto(OrderProductDto updatedProductDto) {
        this.updatedProductDto = updatedProductDto;
    }
}
