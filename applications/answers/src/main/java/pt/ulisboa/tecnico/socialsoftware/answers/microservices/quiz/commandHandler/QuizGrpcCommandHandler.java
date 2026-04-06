package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class QuizGrpcCommandHandler extends GrpcCommandHandler {

    private final QuizCommandHandler quizCommandHandler;

    public QuizGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            QuizCommandHandler quizCommandHandler) {
        super(mapperProvider);
        this.quizCommandHandler = quizCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Quiz";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return quizCommandHandler.handleDomainCommand(command);
    }
}
