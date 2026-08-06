package pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.webapi.requestDtos.CreatePriceConfigRequestDto;

public class CreatePriceConfigCommand extends Command {
    private final CreatePriceConfigRequestDto createRequest;

    public CreatePriceConfigCommand(UnitOfWork unitOfWork, String serviceName, CreatePriceConfigRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreatePriceConfigRequestDto getCreateRequest() { return createRequest; }
}
