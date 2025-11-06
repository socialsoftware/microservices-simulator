package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
@Profile("local")
public class LocalCommandGateway implements CommandGateway {

    private static final Logger logger = Logger.getLogger(LocalCommandGateway.class.getName());
    private final ApplicationContext applicationContext;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    public LocalCommandGateway(ApplicationContext applicationContext, RetryRegistry retryRegistry) {
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
//    @Retry(name = "commandGateway", fallbackMethod = "fallbackSend")
//    @CircuitBreaker(name = "commandGateway", fallbackMethod = "fallbackSend")
    public Object send(Command command) {

        CommandHandler handler = (CommandHandler) applicationContext.getBean(
                command.getServiceName() + "CommandHandler"
        );

        logger.info("Executing command: " + command.getClass().getSimpleName());
        Object returnObject = handler.handle(command);
        if (returnObject instanceof SimulatorException) {
            throw (SimulatorException) returnObject;
        }
        if (returnObject instanceof CannotAcquireLockException) {
           throw (CannotAcquireLockException) returnObject;
        }
        return returnObject;
    }

    @Override
    public CompletableFuture<Object> sendAsync(Command command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(command);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
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
