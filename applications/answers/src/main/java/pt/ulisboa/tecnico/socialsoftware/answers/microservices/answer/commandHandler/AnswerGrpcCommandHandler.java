package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class AnswerGrpcCommandHandler extends GrpcCommandHandler {

    private final AnswerCommandHandler answerCommandHandler;

    public AnswerGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            AnswerCommandHandler answerCommandHandler) {
        super(mapperProvider);
        this.answerCommandHandler = answerCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Answer";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return answerCommandHandler.handleDomainCommand(command);
    }
}
