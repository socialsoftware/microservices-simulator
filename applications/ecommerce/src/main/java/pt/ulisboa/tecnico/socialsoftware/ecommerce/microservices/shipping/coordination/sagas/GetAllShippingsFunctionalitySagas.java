package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.shipping.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllShippingsFunctionalitySagas extends WorkflowFunctionality {
    private List<ShippingDto> shippings;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllShippingsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllShippingsStep = new SagaStep("getAllShippingsStep", () -> {
            GetAllShippingsCommand cmd = new GetAllShippingsCommand(unitOfWork, ServiceMapping.SHIPPING.getServiceName());
            List<ShippingDto> shippings = (List<ShippingDto>) commandGateway.send(cmd);
            setShippings(shippings);
        });

        workflow.addStep(getAllShippingsStep);
    }
    public List<ShippingDto> getShippings() {
        return shippings;
    }

    public void setShippings(List<ShippingDto> shippings) {
        this.shippings = shippings;
    }
}
