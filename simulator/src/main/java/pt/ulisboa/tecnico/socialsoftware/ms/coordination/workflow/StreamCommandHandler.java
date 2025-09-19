package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.logging.Logger;

public abstract class StreamCommandHandler implements CommandHandler {

    private static final Logger logger = Logger.getLogger(StreamCommandHandler.class.getName());
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    protected StreamCommandHandler(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;

        // Create and configure ObjectMapper
        this.objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // objectMapper.activateDefaultTyping(
        //         objectMapper.getPolymorphicTypeValidator(),
        //         ObjectMapper.DefaultTyping.NON_FINAL,
        //         JsonTypeInfo.As.PROPERTY
        // );

        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Use polymorphic typing for domain objects but skip container (Map/Collection) types
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("pt.ulisboa.tecnico") // narrow the allowed subtype space
                .build();

        ObjectMapper.DefaultTypeResolverBuilder typer =
                new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL, ptv) {
                    @Override
                    public boolean useForType(JavaType t) {
                        // Do not require @class for containers; this avoids failures on empty maps
                        if (t.isContainerType() || t.isMapLikeType() || t.isCollectionLikeType()) {
                            return false;
                        }
                        return super.useForType(t);
                    }
                };
        typer = (ObjectMapper.DefaultTypeResolverBuilder) typer.init(JsonTypeInfo.Id.CLASS, null)
                .inclusion(JsonTypeInfo.As.PROPERTY)
                .typeProperty("@class");

        objectMapper.setDefaultTyping(typer);

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
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
        streamBridge.send("command-responses",
                MessageBuilder.withPayload(response).build());
    }

    private void sendErrorResponse(String correlationId, String errorMessage) {
        CommandResponse response = CommandResponse.error(correlationId, errorMessage);
        streamBridge.send("command-responses",
                MessageBuilder.withPayload(response).build());
    }
}
