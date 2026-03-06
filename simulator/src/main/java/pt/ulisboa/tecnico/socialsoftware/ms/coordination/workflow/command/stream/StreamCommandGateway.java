package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandResponse;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;

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
        String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
        String service = command.getServiceName() != null ? command.getServiceName() : "";

        // If appName equals service and a local CommandHandler bean exists, dispatch
        // locally
        String handlerBeanName = command.getServiceName() + "CommandHandler";
        if (service.equals(appName) && applicationContext.containsBean(handlerBeanName)) {
            CommandHandler handler = (CommandHandler) applicationContext.getBean(handlerBeanName);
            logger.info("Dispatching command locally: " + command.getClass().getSimpleName());
            return handler.handle(command);
        }

        String destination = service + "-command-channel";
        String correlationId = java.util.UUID.randomUUID().toString();

        String replyTo = (appName != null && !appName.isEmpty())
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

        CommandResponse resp;
        try {
            resp = responseFuture.get(commandTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new RuntimeException(String.format(
                    "Timeout after %ds waiting for response from service '%s' for command %s",
                    commandTimeoutSeconds, command.getServiceName(), command.getClass().getSimpleName()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for command response", e);
        } catch (Exception e) {
            throw new RuntimeException("Error waiting for command response", e);
        }

        logger.info("Received response for command " + command.getClass().getSimpleName());
        mergeUnitOfWork(command.getUnitOfWork(), resp.unitOfWork());

        if (resp.isError()) {
            throwMatchingException(resp.errorType(), resp.errorMessage());
        }
        return resp.result();
    }
}
