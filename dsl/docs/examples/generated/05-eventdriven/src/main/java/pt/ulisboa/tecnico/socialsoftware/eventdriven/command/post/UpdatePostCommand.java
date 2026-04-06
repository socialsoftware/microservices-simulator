package pt.ulisboa.tecnico.socialsoftware.eventdriven.command.post;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;

public class UpdatePostCommand extends Command {
    private final PostDto postDto;

    public UpdatePostCommand(UnitOfWork unitOfWork, String serviceName, PostDto postDto) {
        super(unitOfWork, serviceName, null);
        this.postDto = postDto;
    }

    public PostDto getPostDto() { return postDto; }
}
