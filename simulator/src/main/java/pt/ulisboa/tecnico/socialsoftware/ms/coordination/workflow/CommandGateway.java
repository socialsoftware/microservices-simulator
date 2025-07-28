package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public abstract class CommandGateway {

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
        //         .forEach((name, handler) -> System.out
        //                 .println("  - " + name + " (" + handler.getClass().getName() + ")"));

        CommandHandler handler = applicationContext.getBeansOfType(CommandHandler.class)
                .values()
                .stream()
                .filter(h -> h.getClass().getSimpleName().equalsIgnoreCase(handlerClassName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No handler found for command: " + handlerClassName));

        try {
            Object returnObject =  handler.handle(command);
            if (returnObject instanceof Exception) {
                throw (Exception) returnObject;
            }
            return returnObject;
        } catch (SimulatorException e) {
            Logger.getLogger(CommandGateway.class.getName()).severe("Error handling command: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            Logger.getLogger(CommandGateway.class.getName()).severe("Unexpected error handling command: " + e.getMessage());
            throw new SimulatorException(e.getMessage());
        }
    }
}
