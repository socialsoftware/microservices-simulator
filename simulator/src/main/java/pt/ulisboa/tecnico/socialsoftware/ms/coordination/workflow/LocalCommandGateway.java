package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
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
    public LocalCommandGateway(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @CircuitBreaker(name = "commandGateway", fallbackMethod = "fallbackSend")
    @Retry(name = "commandGateway")
    public Object send(Command command) {
        String handlerClassName = command.getServiceName() + "CommandHandler";

        CommandHandler handler = (CommandHandler) applicationContext.getBean(
                command.getServiceName() + "CommandHandler"
        );

        try {
            Object returnObject = handler.handle(command);
            if (returnObject instanceof SimulatorException) {
                throw (SimulatorException) returnObject;
            }
            return returnObject;
        } catch (SimulatorException e) {
            logger.warning(e.getMessage());
            throw e;
        }
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
        logger.severe("Circuit breaker opened or retries exhausted for command: "
                + command.getClass().getSimpleName() + " - " + t.getMessage());
        throw new RuntimeException("Service unavailable: " + command.getServiceName(), t);
    }
}
