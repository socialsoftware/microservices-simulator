package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public abstract class CommandGateway {

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final ApplicationContext applicationContext;
    protected final ExecutorService executor = Executors.newCachedThreadPool();

    protected CommandGateway(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected CommandGateway(ApplicationContext applicationContext, RetryRegistry retryRegistry) {
        this.applicationContext = applicationContext;
        configureRetryRegistry(retryRegistry);
    }

    protected void configureRetryRegistry(RetryRegistry retryRegistry) {
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

    public abstract Object send(Command command);

    public CompletableFuture<Object> sendAsync(Command command) {
        return CompletableFuture.supplyAsync(() -> send(command), executor);
    }

    @SuppressWarnings("unused")
    public Object fallbackSend(Command command, Throwable t) {
        if (t instanceof SimulatorException) {
            throw (SimulatorException) t;
        }

        String serviceName = command.getServiceName() != null ? command.getServiceName() : "unknown";
        String commandName = command.getClass().getSimpleName();

        Throwable rootCause = t;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }

        logger.severe(String.format(
                "Retries exhausted for command %s on service '%s'. Cause: %s - %s",
                commandName, serviceName,
                rootCause.getClass().getSimpleName(), rootCause.getMessage()));

        throw new SimulatorException(String.format(
                "Service '%s' unavailable after retries exhausted for %s: %s",
                serviceName, commandName, rootCause.getMessage()));
    }

    protected void mergeUnitOfWork(UnitOfWork target, UnitOfWork source) {
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
