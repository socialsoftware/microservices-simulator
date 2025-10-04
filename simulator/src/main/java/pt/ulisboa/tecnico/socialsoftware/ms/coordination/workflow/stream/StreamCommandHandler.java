package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.logging.Logger;

@Profile("stream")
public abstract class StreamCommandHandler implements CommandHandler {

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

        logger.info("Handling command for service: " + command.getServiceName() +
                " with correlation ID: " + correlationId);

        try {
            Object result = handle(command);
            if (result instanceof Exception) { // TODO check if result is better thrown
                sendErrorResponse(correlationId, ((Exception) result).getMessage());
                return;
            }
            sendResponse(correlationId, result);
        } catch (SimulatorException e) {
            logger.warning("Command handling error: " + e.getMessage());
            sendErrorResponse(correlationId, e.getMessage());
        } catch (Exception e) {
            logger.severe("Unexpected error handling command: " + e.getMessage());
            sendErrorResponse(correlationId, "Unexpected error: " + e.getMessage());
        }
    }

    private void sendResponse(String correlationId, Object result) {
        logger.info("Sending response.....");
        CommandResponse response = CommandResponse.success(correlationId, result);
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sent success response for correlationId=" + correlationId +
                " resultType=" + (result == null ? "null" : result.getClass().getName()));
        streamBridge.send("command-responses", MessageBuilder.withPayload(json).build());
    }

    private void sendErrorResponse(String correlationId, String errorMessage) {
        CommandResponse response = CommandResponse.error(correlationId, errorMessage);
        String json;
        try {
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("Sent error response for correlationId=" + correlationId + " message=" + errorMessage);
        streamBridge.send("command-responses", MessageBuilder.withPayload(json).build());
    }
}
