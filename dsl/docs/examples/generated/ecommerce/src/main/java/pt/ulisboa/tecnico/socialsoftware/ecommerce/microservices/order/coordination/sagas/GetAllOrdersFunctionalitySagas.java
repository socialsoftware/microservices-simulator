package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllOrdersFunctionalitySagas extends WorkflowFunctionality {
    private List<OrderDto> orders;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllOrdersFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllOrdersStep = new SagaStep("getAllOrdersStep", () -> {
            GetAllOrdersCommand cmd = new GetAllOrdersCommand(unitOfWork, ServiceMapping.ORDER.getServiceName());
            List<OrderDto> orders = (List<OrderDto>) commandGateway.send(cmd);
            setOrders(orders);
        });

        workflow.addStep(getAllOrdersStep);
    }
    public List<OrderDto> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderDto> orders) {
        this.orders = orders;
    }
}
