package pt.ulisboa.tecnico.socialsoftware.ms.messaging.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandReply;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandRequest;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandServiceGrpc;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

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
        String correlationId = request.getCorrelationId();
        Command command;

        try {
            command = objectMapper.readValue(request.getCommandJson(), Command.class);
        } catch (Exception e) {
            logger.severe("Failed to deserialize command: " + e.getMessage());
            sendResponse(correlationId, null, null, responseObserver, e);
            return;
        }

        logger.info("Received gRPC command for service: " + command.getServiceName());

        CommandHandler handler;
        try {
            handler = (CommandHandler) applicationContext.getBean(command.getServiceName() + "CommandHandler");
        } catch (Exception e) {
            logger.severe("Failed to find command handler for service: " + command.getServiceName());
            sendResponse(correlationId, null, null, responseObserver, e);
            return;
        }

        logger.info("Delegating gRPC command to handler for service: " + command.getServiceName());

        try {
            Object result = handler.handle(command);
            sendResponse(correlationId, result, command.getUnitOfWork(), responseObserver, null);
        } catch (SimulatorException e) {
            logger.warning("Command handling error: " + e.getMessage());
            sendResponse(correlationId, null, command.getUnitOfWork(), responseObserver, e);
        } catch (Exception e) {
            logger.severe(
                    "Unexpected error handling command: " + e.getMessage() + " " + command.getClass().getSimpleName());
            sendResponse(correlationId, null, command.getUnitOfWork(), responseObserver, e);
        }
    }

    private void sendResponse(String correlationId, Object result, UnitOfWork unitOfWork,
            StreamObserver<CommandReply> responseObserver, Exception exception) {
        logger.info("Sending gRPC response.....");

        CommandResponse response;
        if (exception != null) {
            logger.severe("Error sending response: " + exception.getMessage());
            response = CommandResponse.error(correlationId, exception, unitOfWork);
        } else {
            response = CommandResponse.success(correlationId, result, unitOfWork);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sent response for correlationId=" + correlationId +
                " isError=" + response.isError());
        responseObserver.onNext(CommandReply.newBuilder().setResponseJson(json).build());
        responseObserver.onCompleted();
    }
}
