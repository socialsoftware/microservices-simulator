package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class MemberGrpcCommandHandler extends GrpcCommandHandler {

    private final MemberCommandHandler memberCommandHandler;

    public MemberGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            MemberCommandHandler memberCommandHandler) {
        super(mapperProvider);
        this.memberCommandHandler = memberCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Member";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return memberCommandHandler.handleDomainCommand(command);
    }
}
