package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.logging.Logger;

@Component
public class CommandGateway {

    private final ApplicationContext applicationContext;

    @Autowired
    public CommandGateway(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object send(Command command) {
        String handlerClassName = command.getServiceName() + "CommandHandler";
        // Log all registered CommandHandler beans
        // System.out.println("CommandHandler beans:");
        // applicationContext.getBeansOfType(CommandHandler.class)
        // .forEach((name, handler) -> System.out
        // .println(" - " + name + " (" + handler.getClass().getName() + ")"));

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
}
