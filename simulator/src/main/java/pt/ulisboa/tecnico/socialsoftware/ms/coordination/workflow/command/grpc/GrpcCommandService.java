package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandReply;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandRequest;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandServiceGrpc;

import java.util.logging.Logger;

@Component
@Profile("grpc")
public class GrpcCommandService extends CommandServiceGrpc.CommandServiceImplBase {
    private static final Logger logger = Logger.getLogger(GrpcCommandService.class.getName());

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Autowired
    public GrpcCommandService(ApplicationContext applicationContext, MessagingObjectMapperProvider mapperProvider) {
        this.applicationContext = applicationContext;
        this.objectMapper = mapperProvider.newMapper();
    }

    @Override
    public void send(CommandRequest request, StreamObserver<CommandReply> responseObserver) {
        try {
            Command command = objectMapper.readValue(request.getCommandJson(), Command.class);
            logger.info("Received gRPC command for service: " + command.getServiceName() + "for bean grpc" + command.getServiceName() + "CommandHandler");
            GrpcCommandHandler handler = (GrpcCommandHandler) applicationContext
                    .getBean(command.getServiceName() + "GrpcCommandHandler");

            logger.info("Delegating gRPC command to handler for service: " + command.getServiceName());
            handler.handleGrpcRequest(request, responseObserver);
        } catch (Exception e) {
            logger.severe("Failed to route gRPC command: " + e.getMessage());
            responseObserver.onError(e);
        }
    }
}
