package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class CommandGateway {
//    private static CommandGateway instance;
    private final ApplicationContext applicationContext;

//    public static CommandGateway getInstance(ApplicationContext applicationContext) {
//        if (CommandGateway.instance == null) { CommandGateway.instance = new CommandGateway(applicationContext); }
//        return CommandGateway.instance;
//    }

    @Autowired
    public CommandGateway(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void send(Command command) {
        String handlerClassName = command.getClass().getSimpleName().replace("Command", "CommandHandler");
        CommandHandler<Command> handler =
                (CommandHandler<Command>) applicationContext.getBeansOfType(CommandHandler.class)
                        .values()
                        .stream()
                        .filter(h -> h.getClass().getSimpleName().equals(handlerClassName))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No handler found for command: " + command.getClass().getName()));

        handler.handle(command);
    }
}
