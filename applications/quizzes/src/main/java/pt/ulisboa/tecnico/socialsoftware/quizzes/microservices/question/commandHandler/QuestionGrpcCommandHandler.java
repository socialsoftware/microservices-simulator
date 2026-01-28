package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;

@Component
@Profile("grpc")
public class QuestionGrpcCommandHandler extends GrpcCommandHandler {

    private final QuestionCommandHandler questionCommandHandler;

    public QuestionGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            QuestionCommandHandler questionCommandHandler) {
        super(mapperProvider);
        this.questionCommandHandler = questionCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Question";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return questionCommandHandler.handleDomainCommand(command);
    }
}
