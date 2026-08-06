package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.station.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class GetStationByIdFunctionalitySagas extends WorkflowFunctionality {
    private StationDto stationDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetStationByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer stationAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(stationAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer stationAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStationStep = new SagaStep("getStationStep", () -> {
            GetStationByIdCommand cmd = new GetStationByIdCommand(unitOfWork, ServiceMapping.STATION.getServiceName(), stationAggregateId);
            StationDto stationDto = (StationDto) commandGateway.send(cmd);
            setStationDto(stationDto);
        });

        workflow.addStep(getStationStep);
    }
    public StationDto getStationDto() {
        return stationDto;
    }

    public void setStationDto(StationDto stationDto) {
        this.stationDto = stationDto;
    }
}
