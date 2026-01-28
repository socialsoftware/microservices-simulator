package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;

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
        return "QuizAnswer";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return answerCommandHandler.handleDomainCommand(command);
    }
}
