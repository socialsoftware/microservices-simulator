package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;

@Component
@Profile("grpc")
public class TopicGrpcCommandHandler extends GrpcCommandHandler {

    private final TopicCommandHandler topicCommandHandler;

    public TopicGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            TopicCommandHandler topicCommandHandler) {
        super(mapperProvider);
        this.topicCommandHandler = topicCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Topic";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return topicCommandHandler.handleDomainCommand(command);
    }
}
