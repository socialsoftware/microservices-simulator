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

public class GetDiscountByIdFunctionalitySagas extends WorkflowFunctionality {
    private DiscountDto discountDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetDiscountByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer discountAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(discountAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer discountAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getDiscountStep = new SagaStep("getDiscountStep", () -> {
            GetDiscountByIdCommand cmd = new GetDiscountByIdCommand(unitOfWork, ServiceMapping.DISCOUNT.getServiceName(), discountAggregateId);
            DiscountDto discountDto = (DiscountDto) commandGateway.send(cmd);
            setDiscountDto(discountDto);
        });

        workflow.addStep(getDiscountStep);
    }
    public DiscountDto getDiscountDto() {
        return discountDto;
    }

    public void setDiscountDto(DiscountDto discountDto) {
        this.discountDto = discountDto;
    }
}
