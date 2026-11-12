package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.CreatePriceConfigCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;

// CreatePriceConfig is a single-aggregate write: the route and train type it names are stored as
// supplied and nothing fetches them, so the saga has no data-assembly step.
public class CreatePriceConfigFunctionalitySagas extends WorkflowFunctionality {
    private PriceConfigDto createdPriceConfigDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreatePriceConfigFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               PriceConfigDto priceConfigDto,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(priceConfigDto, unitOfWork);
    }

    public void buildWorkflow(PriceConfigDto priceConfigDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createPriceConfigStep = new SagaStep("createPriceConfigStep", () -> {
            CreatePriceConfigCommand cmd = new CreatePriceConfigCommand(
                    unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(), priceConfigDto);
            this.createdPriceConfigDto = (PriceConfigDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(createPriceConfigStep);
    }

    public PriceConfigDto getCreatedPriceConfigDto() {
        return createdPriceConfigDto;
    }
}
