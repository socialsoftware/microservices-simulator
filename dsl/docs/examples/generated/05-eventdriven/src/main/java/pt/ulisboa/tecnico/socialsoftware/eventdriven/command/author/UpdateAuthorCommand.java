package pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;

public class UpdateAuthorCommand extends Command {
    private final AuthorDto authorDto;

    public UpdateAuthorCommand(UnitOfWork unitOfWork, String serviceName, AuthorDto authorDto) {
        super(unitOfWork, serviceName, null);
        this.authorDto = authorDto;
    }

    public AuthorDto getAuthorDto() { return authorDto; }
}
