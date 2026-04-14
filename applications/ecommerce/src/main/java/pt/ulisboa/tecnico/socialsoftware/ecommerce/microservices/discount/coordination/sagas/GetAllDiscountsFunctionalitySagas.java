package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.discount.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllDiscountsFunctionalitySagas extends WorkflowFunctionality {
    private List<DiscountDto> discounts;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllDiscountsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllDiscountsStep = new SagaStep("getAllDiscountsStep", () -> {
            GetAllDiscountsCommand cmd = new GetAllDiscountsCommand(unitOfWork, ServiceMapping.DISCOUNT.getServiceName());
            List<DiscountDto> discounts = (List<DiscountDto>) commandGateway.send(cmd);
            setDiscounts(discounts);
        });

        workflow.addStep(getAllDiscountsStep);
    }
    public List<DiscountDto> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(List<DiscountDto> discounts) {
        this.discounts = discounts;
    }
}
