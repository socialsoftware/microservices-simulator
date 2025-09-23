package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.function.Consumer;
import java.util.logging.Logger;

@Component
@Profile("stream")
public class CommandResponseListener {

    private static final Logger logger = Logger.getLogger(CommandResponseListener.class.getName());
    private final CommandResponseAggregator responseAggregator;
    private final ObjectMapper objectMapper;

    @Autowired
    public CommandResponseListener(CommandResponseAggregator responseAggregator,
                                   MessagingObjectMapperProvider mapperProvider) {
        this.responseAggregator = responseAggregator;
        this.objectMapper = mapperProvider.newMapper();
    }

    @Bean
    public Consumer<Message<?>> commandResponseChannel() {
        logger.info("CommandResponseListener: Starting command response channel");
        return message -> {
            CommandResponse response;

            Object payload = message.getPayload();
            if (payload instanceof CommandResponse) {
                response = (CommandResponse) payload;
            } else if (payload instanceof byte[]) {
                try {
                    response = objectMapper.readValue((byte[]) payload, CommandResponse.class);
                    logger.info("Successfully deserialized CommandResponse from byte array");
                } catch (Exception e) {
                    logger.severe("Failed to deserialize CommandResponse: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } else if (payload instanceof String) {
                try {
                    response = objectMapper.readValue((String) payload, CommandResponse.class);
                    logger.info("Successfully deserialized CommandResponse from String");
                } catch (Exception e) {
                    logger.severe("Failed to deserialize CommandResponse from String: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } else {
                logger.severe("Unsupported payload type for CommandResponse: " + payload.getClass());
                return;
            }

            String headerCorrelationId = (String) message.getHeaders().get("correlationId");
            String correlationId = response.correlationId() != null ? response.correlationId() : headerCorrelationId;

            logger.info("Received response for correlation ID: " + correlationId);

            if (response.isError()) {
                responseAggregator.completeExceptionally(
                        correlationId,
                        new SimulatorException(response.errorMessage()));
            } else {
                responseAggregator.completeResponse(
                        correlationId,
                        response.result());
            }
        };
    }
}
