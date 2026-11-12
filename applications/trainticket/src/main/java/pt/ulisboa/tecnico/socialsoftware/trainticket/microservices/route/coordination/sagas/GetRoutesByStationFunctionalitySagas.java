package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRoutesByStationCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;

import java.util.List;

public class GetRoutesByStationFunctionalitySagas extends WorkflowFunctionality {
    private List<RouteDto> routes;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetRoutesByStationFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                Integer stationAggregateId,
                                                SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(stationAggregateId, unitOfWork);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(Integer stationAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getRoutesStep = new SagaStep("getRoutesStep", () -> {
            GetRoutesByStationCommand cmd = new GetRoutesByStationCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), stationAggregateId);
            this.routes = (List<RouteDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getRoutesStep);
    }

    public List<RouteDto> getRoutes() {
        return routes;
    }
}
