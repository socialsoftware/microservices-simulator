package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.train.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllTrainsFunctionalitySagas extends WorkflowFunctionality {
    private List<TrainDto> trains;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllTrainsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllTrainsStep = new SagaStep("getAllTrainsStep", () -> {
            GetAllTrainsCommand cmd = new GetAllTrainsCommand(unitOfWork, ServiceMapping.TRAIN.getServiceName());
            List<TrainDto> trains = (List<TrainDto>) commandGateway.send(cmd);
            setTrains(trains);
        });

        workflow.addStep(getAllTrainsStep);
    }
    public List<TrainDto> getTrains() {
        return trains;
    }

    public void setTrains(List<TrainDto> trains) {
        this.trains = trains;
    }
}
