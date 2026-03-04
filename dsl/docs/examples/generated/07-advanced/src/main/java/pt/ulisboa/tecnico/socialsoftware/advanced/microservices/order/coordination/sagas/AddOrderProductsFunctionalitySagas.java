package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddOrderProductsFunctionalitySagas extends WorkflowFunctionality {
    private List<OrderProductDto> addedProductDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddOrderProductsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer orderId, List<OrderProductDto> productDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(orderId, productDtos, unitOfWork);
    }

    public void buildWorkflow(Integer orderId, List<OrderProductDto> productDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addProductsStep = new SagaStep("addProductsStep", () -> {
            AddOrderProductsCommand cmd = new AddOrderProductsCommand(unitOfWork, ServiceMapping.ORDER.getServiceName(), orderId, productDtos);
            List<OrderProductDto> addedProductDtos = (List<OrderProductDto>) commandGateway.send(cmd);
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
