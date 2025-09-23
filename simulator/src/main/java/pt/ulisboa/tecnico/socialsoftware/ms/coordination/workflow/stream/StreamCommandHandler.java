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

    // New constructor using the shared provider
    protected StreamCommandHandler(StreamBridge streamBridge, MessagingObjectMapperProvider mapperProvider) {
        this.streamBridge = streamBridge;
        this.objectMapper = mapperProvider.newMapper();
    }

    public void handleCommandMessage(Message<?> message) {
        Command command;

        // Handle the case where the message payload is not a Command object
        if (!(message.getPayload() instanceof Command)) {
            logger.warning("Received message payload is not a Command object: " + message.getPayload().getClass());

            if (message.getPayload() instanceof byte[]) {
                try {
                    // Use the pre-configured ObjectMapper
                    command = objectMapper.readValue((byte[]) message.getPayload(), Command.class);
                    logger.info("Successfully deserialized command from byte array");
                } catch (Exception e) {
                    logger.severe("Failed to deserialize command: " + e.getMessage());
                    e.printStackTrace(); // Add stack trace for more detailed debugging
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
            // Serialize with the messaging mapper (includes @class for nested DTOs)
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info(json);
        streamBridge.send("command-responses",
                MessageBuilder.withPayload(json).build());
    }

    private void sendErrorResponse(String correlationId, String errorMessage) {
        CommandResponse response = CommandResponse.error(correlationId, errorMessage);
        String json;
        try {
            // Serialize with the messaging mapper (includes @class for nested DTOs)
            json = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info(json);
        streamBridge.send("command-responses",
                MessageBuilder.withPayload(json).build());
    }
}
