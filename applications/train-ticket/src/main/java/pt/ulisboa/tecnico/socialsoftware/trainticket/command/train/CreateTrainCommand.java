package pt.ulisboa.tecnico.socialsoftware.trainticket.command.train;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.webapi.requestDtos.CreateTrainRequestDto;

public class CreateTrainCommand extends Command {
    private final CreateTrainRequestDto createRequest;

    public CreateTrainCommand(UnitOfWork unitOfWork, String serviceName, CreateTrainRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateTrainRequestDto getCreateRequest() { return createRequest; }
}
