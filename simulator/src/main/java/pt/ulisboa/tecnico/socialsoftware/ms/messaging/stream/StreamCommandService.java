package pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

import java.util.logging.Logger;

@Component
@Profile("stream")
public class StreamCommandService {

    private static final Logger logger = Logger.getLogger(StreamCommandService.class.getName());
    private final ApplicationContext applicationContext;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    @Autowired
    public StreamCommandService(ApplicationContext applicationContext, StreamBridge streamBridge,
                                MessagingObjectMapperProvider mapperProvider) {
        this.applicationContext = applicationContext;
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

        CommandHandler handler;
        try {
            handler = (CommandHandler) applicationContext.getBean(command.getServiceName() + "CommandHandler");
        } catch (Exception e) {
            logger.severe("Failed to find command handler for service: " + command.getServiceName());
            return;
        }

        try {
            Object result = handler.handle(command);
            sendResponse(correlationId, result, command.getUnitOfWork(), replyTo, null);
        } catch (SimulatorException e) {
            logger.warning("Command handling error: " + e.getMessage());
            sendResponse(correlationId, null, command.getUnitOfWork(), replyTo, e);
        } catch (Exception e) {
            logger.severe(
                    "Unexpected error handling command: " + e.getMessage() + " " + command.getClass().getSimpleName());
            sendResponse(correlationId, null, command.getUnitOfWork(), replyTo, e);
        }
    }

    private void sendResponse(String correlationId, Object result, UnitOfWork unitOfWork, String replyTo, Exception exception) {
        logger.info("Sending response.....");
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
        logger.info("Sent success response for correlationId=" + correlationId +
                " resultType=" + (result == null ? "null" : result.getClass().getName()));
        streamBridge.send(replyTo, MessageBuilder.withPayload(json).build());
    }
}
