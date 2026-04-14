package pt.ulisboa.tecnico.socialsoftware.showcase.command.booking;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto;

public class CreateBookingCommand extends Command {
    private final CreateBookingRequestDto createRequest;

    public CreateBookingCommand(UnitOfWork unitOfWork, String serviceName, CreateBookingRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateBookingRequestDto getCreateRequest() { return createRequest; }
}
