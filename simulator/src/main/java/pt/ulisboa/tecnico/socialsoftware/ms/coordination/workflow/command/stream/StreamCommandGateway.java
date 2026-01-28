package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandResponse;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.concurrent.CompletableFuture;

@Component
@Profile("stream")
public class StreamCommandGateway extends CommandGateway {

    private final StreamBridge streamBridge;
    private final CommandResponseAggregator responseAggregator;
    private final ObjectMapper msgMapper;

    @Autowired
    public StreamCommandGateway(StreamBridge streamBridge,
            CommandResponseAggregator responseAggregator,
            MessagingObjectMapperProvider mapperProvider,
            ApplicationContext applicationContext, RetryRegistry retryRegistry) {
        super(applicationContext, retryRegistry);
        this.streamBridge = streamBridge;
        this.responseAggregator = responseAggregator;
        this.msgMapper = mapperProvider.newMapper();
    }

    @Override
    @Retry(name = "commandGateway", fallbackMethod = "fallbackSend")
    public Object send(Command command) {
        String service = command.getServiceName() != null ? command.getServiceName() : "";

        String destination = service + "-command-channel";
        String correlationId = java.util.UUID.randomUUID().toString();

        String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        String replyTo = (appName != null && !appName.isEmpty() && !appName.equals("quizzes"))
                ? appName + "-command-responses"
                : "command-responses";

        CompletableFuture<CommandResponse> responseFuture = responseAggregator.createResponseFuture(correlationId);
        logger.info("Sending command " + command.getClass().getSimpleName() + " to " + destination);
        String json;
        try {
            json = msgMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        boolean sent = streamBridge.send(destination,
                MessageBuilder.withPayload(json)
                        .setHeader("correlationId", correlationId)
                        .setHeader("contentType", "application/json")
                        .setHeader("replyTo", replyTo)
                        .build());

        if (!sent) {
            throw new RuntimeException("Failed to send command to service: " + command.getServiceName());
        }

        logger.info("Command sent to " + destination);

        try {
            CommandResponse resp = responseFuture.get();
            logger.info("Received response for command " + command.getClass().getSimpleName());
            mergeUnitOfWork(command.getUnitOfWork(), resp.unitOfWork());
            Object result = resp.result();
            if (result instanceof SimulatorException) {
                throw (SimulatorException) result;
            } else if (result instanceof TransientDataAccessException) {
                throw (TransientDataAccessException) result;
            }
            return result;
        } catch (Exception e) {
            logger.warning("Error while waiting for response: " + e.getMessage());
            throw new RuntimeException("Error processing command", e);
        }
    }
}
