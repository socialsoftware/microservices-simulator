package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.GetStationByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;

public class GetStationByIdFunctionalitySagas extends WorkflowFunctionality {
    private StationDto stationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetStationByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer stationAggregateId,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(stationAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer stationAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStationStep = new SagaStep("getStationStep", () -> {
            GetStationByIdCommand cmd = new GetStationByIdCommand(
                    unitOfWork, ServiceMapping.STATION.getServiceName(), stationAggregateId);
            this.stationDto = (StationDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getStationStep);
    }

    public StationDto getStationDto() {
        return stationDto;
    }
}
