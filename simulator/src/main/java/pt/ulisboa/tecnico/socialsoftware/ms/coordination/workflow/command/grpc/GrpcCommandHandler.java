package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.springframework.context.annotation.Profile;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandReply;
import pt.ulisboa.tecnico.socialsoftware.ms.grpc.CommandRequest;

import java.util.logging.Logger;

@Profile("grpc")
public abstract class GrpcCommandHandler extends CommandHandler {

    private static final Logger logger = Logger.getLogger(GrpcCommandHandler.class.getName());
    private final ObjectMapper objectMapper;

    protected GrpcCommandHandler(MessagingObjectMapperProvider mapperProvider) {
        this.objectMapper = mapperProvider.newMapper();
    }

    public void handleGrpcRequest(CommandRequest request, StreamObserver<CommandReply> responseObserver) {
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

        logger.info("Handling gRPC command for service: " + command.getServiceName() +
                " with correlation ID: " + correlationId);

        try {
            Object result = handle(command);
            if (result instanceof Exception e) {
                sendErrorResponse(correlationId, e.getMessage(), command.getUnitOfWork(), responseObserver);
                return;
            }
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
