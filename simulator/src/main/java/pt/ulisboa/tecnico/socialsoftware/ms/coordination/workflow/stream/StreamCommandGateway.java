package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

import java.beans.Transient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
@Profile("stream")
public class StreamCommandGateway implements CommandGateway {
    private static final Logger logger = Logger.getLogger(StreamCommandGateway.class.getName());
    private final StreamBridge streamBridge;
    private final CommandResponseAggregator responseAggregator;
    private final ObjectMapper msgMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ApplicationContext applicationContext;

    @Autowired
    public StreamCommandGateway(StreamBridge streamBridge,
                                CommandResponseAggregator responseAggregator,
                                MessagingObjectMapperProvider mapperProvider,
                                ApplicationContext applicationContext, RetryRegistry retryRegistry) {
        this.streamBridge = streamBridge;
        this.responseAggregator = responseAggregator;
        this.msgMapper = mapperProvider.newMapper();
        this.applicationContext = applicationContext;

        retryRegistry.retry("commandGateway")
                .getEventPublisher()
                .onRetry(event -> {
                    assert event.getLastThrowable() != null;
                    logger.warning(String.format("Retry attempt #%d for operation. Reason: %s - %s",
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable().getClass().getSimpleName(),
                            event.getLastThrowable().getMessage()));
                })
                .onSuccess(event -> {
                    if (event.getNumberOfRetryAttempts() > 0) {
                        logger.info(String.format("Operation succeeded after %d retry attempts",
                                event.getNumberOfRetryAttempts()));
                    }
                });
    }

    @Override
    @Retry(name = "commandGateway", fallbackMethod = "fallbackSend")
    public Object send(Command command) {
        String service = command.getServiceName() != null ? command.getServiceName().toLowerCase() : "";

        String destination = service + "-command-channel";
        String correlationId = java.util.UUID.randomUUID().toString();

        CompletableFuture<CommandResponse> responseFuture = responseAggregator.createResponseFuture(correlationId);
        logger.info("Sending command to " + destination);
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
                        .build());

        if (!sent) {
            throw new RuntimeException("Failed to send command to service: " + command.getServiceName());
        }

        logger.info("Command sent to " + destination);

        try {
            CommandResponse resp = responseFuture.get();
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

    public CompletableFuture<Object> sendAsync(Command command) {
        return CompletableFuture.supplyAsync(() -> send(command), executor);
    }

    private void mergeUnitOfWork(UnitOfWork target, UnitOfWork source) {
        if (target == null || source == null)
            return;
        if (source.getId() != null)
            target.setId(source.getId());
        if (source.getVersion() != null)
            target.setVersion(source.getVersion());
        if (source.getAggregatesToCommit() != null)
            target.getAggregatesToCommit().putAll(source.getAggregatesToCommit());
        if (source.getEventsToEmit() != null)
            target.getEventsToEmit().addAll(source.getEventsToEmit());

        if (target instanceof SagaUnitOfWork t && source instanceof SagaUnitOfWork s) {
            if (s.getAggregatesInSaga() != null) {
                for (Integer a : s.getAggregatesInSaga()) {
                    if (!t.getAggregatesInSaga().contains(a)) {
                        t.getAggregatesInSaga().add(a);
                    }
                }
            }
            if (s.getPreviousStates() != null) {
                t.getPreviousStates().putAll(s.getPreviousStates());
            }
        }
    }

    public Object fallbackSend(Command command, Throwable t) {
        if (t instanceof SimulatorException) {
            logger.severe("fallback: Command failed with business error: "
                    + command.getClass().getSimpleName() + " - " + t.getMessage());
            throw (SimulatorException) t;
        } else {
            logger.severe("Retries exhausted for command: "
                    + command.getClass().getSimpleName() + " - " + t.getMessage());
            throw new RuntimeException("Service unavailable: " + command.getServiceName(), t);
        }
    }
}
