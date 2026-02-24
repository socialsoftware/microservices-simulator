package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

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
        String replyTo = (appName != null && !appName.isEmpty()) ? appName + "-command-responses" : "command-responses";

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

    private void mergeUnitOfWork(UnitOfWork target, UnitOfWork source) {
        if (target == null || source == null)
            return;
        if (source.getId() != null)
            target.setId(source.getId());
        if (source.getVersion() != null)
            target.setVersion(source.getVersion());
        if (source.getAggregatesToCommit() != null) {
            for (Aggregate sourceAgg : source.getAggregatesToCommit()) {
                boolean alreadyExists = target.getAggregatesToCommit().stream()
                        .anyMatch(targetAgg -> targetAgg.getAggregateType().equals(sourceAgg.getAggregateType())
                                && targetAgg.getAggregateId().equals(sourceAgg.getAggregateId()));
                if (!alreadyExists) {
                    target.getAggregatesToCommit().add(sourceAgg);
                }
            }
        }
        if (source.getEventsToEmit() != null)
            target.getEventsToEmit().addAll(source.getEventsToEmit());
        logger.info("Merging UnitOfWork - target aggregatesToCommit after: " +
                (target.getAggregatesToCommit() != null
                        ? target.getAggregatesToCommit().size() + " aggregates"
                        : "null"));

        if (target instanceof SagaUnitOfWork t && source instanceof SagaUnitOfWork s) {
            if (s.getAggregatesInSaga() != null) {
                s.getAggregatesInSaga().forEach((aggregateId, aggregateType) -> {
                    if (!t.getAggregatesInSaga().containsKey(aggregateId)) {
                        t.getAggregatesInSaga().put(aggregateId, aggregateType);
                    }
                });
            }
            if (s.getPreviousStates() != null) {
                t.getPreviousStates().putAll(s.getPreviousStates());
            }
        }
    }
}
