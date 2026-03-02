package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
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
        String correlationId = request.getCorrelationId();
        Command command;

        try {
            command = objectMapper.readValue(request.getCommandJson(), Command.class);
        } catch (Exception e) {
            logger.severe("Failed to deserialize command: " + e.getMessage());
            sendErrorResponse(correlationId, "Failed to deserialize command: " + e.getMessage(), null,
                    responseObserver);
            return;
        }

        logger.info("Received gRPC command for service: " + command.getServiceName());

        CommandHandler handler;
        try {
            handler = (CommandHandler) applicationContext.getBean(command.getServiceName() + "CommandHandler");
        } catch (Exception e) {
            logger.severe("Failed to find command handler for service: " + command.getServiceName());
            sendErrorResponse(correlationId, "No handler found for service: " + command.getServiceName(),
                    null, responseObserver);
            return;
        }

        logger.info("Delegating gRPC command to handler for service: " + command.getServiceName());

        try {
            Object result = handler.handle(command);
            sendResponse(correlationId, result, command.getUnitOfWork(), responseObserver);
        } catch (SimulatorException e) {
            logger.warning("Command handling error: " + e.getMessage());
            sendErrorResponse(correlationId, e.getMessage(), command.getUnitOfWork(), responseObserver);
        } catch (Exception e) {
            logger.severe(
                    "Unexpected error handling command: " + e.getMessage() + " " + command.getClass().getSimpleName());
            sendErrorResponse(correlationId, "Unexpected error: " + e.getMessage(), command.getUnitOfWork(),
                    responseObserver);
        }
    }

    private void sendResponse(String correlationId, Object result, UnitOfWork unitOfWork,
            StreamObserver<CommandReply> responseObserver) {
        logger.info("Sending gRPC response.....");
        CommandResponse response = CommandResponse.success(correlationId, result, unitOfWork);
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sent success response for correlationId=" + correlationId +
                " resultType=" + (result == null ? "null" : result.getClass().getName()));
        responseObserver.onNext(CommandReply.newBuilder().setResponseJson(json).build());
        responseObserver.onCompleted();
    }

    private void sendErrorResponse(String correlationId, String errorMessage, UnitOfWork unitOfWork,
            StreamObserver<CommandReply> responseObserver) {
        CommandResponse response = CommandResponse.error(correlationId, errorMessage, unitOfWork);
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sent error response for correlationId=" + correlationId + " message=" + errorMessage);
        responseObserver.onNext(CommandReply.newBuilder().setResponseJson(json).build());
        responseObserver.onCompleted();
    }
}
