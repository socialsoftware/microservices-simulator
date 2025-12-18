package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.logging.Logger;

@Profile("stream")
public abstract class StreamCommandHandler extends CommandHandler {

    private static final Logger logger = Logger.getLogger(StreamCommandHandler.class.getName());
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    protected StreamCommandHandler(StreamBridge streamBridge, MessagingObjectMapperProvider mapperProvider) {
        this.streamBridge = streamBridge;
        this.objectMapper = mapperProvider.newMapper();
    }

    public void handleCommandMessage(Message<?> message) {
        Command command;
        if (!(message.getPayload() instanceof Command)) {
            if (message.getPayload() instanceof byte[]) {
                try {
                    command = objectMapper.readValue((byte[]) message.getPayload(), Command.class);
                } catch (Exception e) {
                    logger.severe("Failed to deserialize command: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } else {
                logger.severe("Unsupported payload type: " + message.getPayload().getClass());
                return;
            }
        } else {
            command = (Command) message.getPayload();
        }

        String correlationId = (String) message.getHeaders().get("correlationId");
        String replyTo = (String) message.getHeaders().get("replyTo");
        if (replyTo == null) {
            replyTo = "command-responses";
        }

        logger.info("Handling command for service: " + command.getServiceName() +
                " with correlation ID: " + correlationId);

        try {
            Object result = handle(command);
            if (result instanceof Exception) { // TODO check if result is better thrown
                sendErrorResponse(correlationId, ((Exception) result).getMessage(), command.getUnitOfWork(), replyTo);
                return;
            }
            sendResponse(correlationId, result, command.getUnitOfWork(), replyTo);
        } catch (SimulatorException e) {
            logger.warning("Command handling error: " + e.getMessage());
            sendErrorResponse(correlationId, e.getMessage(), command.getUnitOfWork(), replyTo);
        } catch (Exception e) {
            logger.severe(
                    "Unexpected error handling command: " + e.getMessage() + " " + command.getClass().getSimpleName());
            sendErrorResponse(correlationId, "Unexpected error: " + e.getMessage(), command.getUnitOfWork(), replyTo);
        }
    }

    private void sendResponse(String correlationId, Object result, UnitOfWork unitOfWork, String replyTo) {
        logger.info("Sending response.....");
        logger.info("UnitOfWork aggregatesToCommit before sending: " +
                (unitOfWork != null && unitOfWork.getAggregatesToCommit() != null
                        ? unitOfWork.getAggregatesToCommit().size() + " aggregates"
                        : "null"));
        CommandResponse response = CommandResponse.success(correlationId, result, unitOfWork);
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sent success response for correlationId=" + correlationId +
                " resultType=" + (result == null ? "null" : result.getClass().getName()));
        streamBridge.send(replyTo, MessageBuilder.withPayload(json).build());
    }

    private void sendErrorResponse(String correlationId, String errorMessage, UnitOfWork unitOfWork, String replyTo) {
        CommandResponse response = CommandResponse.error(correlationId, errorMessage, unitOfWork);
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sent error response for correlationId=" + correlationId + " message=" + errorMessage);
        streamBridge.send(replyTo, MessageBuilder.withPayload(json).build());
    }
}
