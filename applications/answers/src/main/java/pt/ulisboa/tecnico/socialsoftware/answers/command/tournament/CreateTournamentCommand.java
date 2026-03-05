package pt.ulisboa.tecnico.socialsoftware.answers.command.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto;

public class CreateTournamentCommand extends Command {
    private final CreateTournamentRequestDto createRequest;

    public CreateTournamentCommand(UnitOfWork unitOfWork, String serviceName, CreateTournamentRequestDto createRequest) {
        super(unitOfWork, serviceName, null);
        this.createRequest = createRequest;
    }

    public CreateTournamentRequestDto getCreateRequest() { return createRequest; }
}
