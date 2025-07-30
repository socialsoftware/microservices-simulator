package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
public class CommandGateway {

    private final ApplicationContext applicationContext;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    public CommandGateway(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object send(Command command) {
        String handlerClassName = command.getServiceName() + "CommandHandler";

        CommandHandler handler = applicationContext.getBeansOfType(CommandHandler.class)
                .values()
                .stream()
                .filter(h -> h.getClass().getSimpleName().equalsIgnoreCase(handlerClassName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No handler found for command: " + handlerClassName));

        try {
            Object returnObject = handler.handle(command);
            if (returnObject instanceof SimulatorException) {
                throw (SimulatorException) returnObject;
            }
            return returnObject;
        } catch (SimulatorException e) {
            Logger.getLogger(CommandGateway.class.getName()).warning(e.getMessage());
            throw e;
        }
    }

    public CompletableFuture<Object> sendAsync(Command command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(command);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
}
