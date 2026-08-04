package pt.ulisboa.tecnico.socialsoftware.trainticket.command.route;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;

public class UpdateRouteCommand extends Command {
    private final RouteDto routeDto;

    public UpdateRouteCommand(UnitOfWork unitOfWork, String serviceName, RouteDto routeDto) {
        super(unitOfWork, serviceName, null);
        this.routeDto = routeDto;
    }

    public RouteDto getRouteDto() { return routeDto; }
}
