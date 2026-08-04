package pt.ulisboa.tecnico.socialsoftware.trainticket.command.route;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.webapi.requestDtos.CreateRouteRequestDto;

public class CreateRouteCommand extends Command {
    private final CreateRouteRequestDto createRequest;

    public CreateRouteCommand(UnitOfWork unitOfWork, String serviceName, CreateRouteRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateRouteRequestDto getCreateRequest() { return createRequest; }
}
