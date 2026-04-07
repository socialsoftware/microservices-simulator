package pt.ulisboa.tecnico.socialsoftware.ms.messaging.local;

import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;

@Component
@Profile("local")
public class LocalCommandGateway extends CommandGateway {

    @Autowired
    public LocalCommandGateway(ApplicationContext applicationContext, RetryRegistry retryRegistry) {
        super(applicationContext, retryRegistry);
    }

    @Override
    @Retry(name = "commandGateway", fallbackMethod = "fallbackSend")
    public Object send(Command command) {
        CommandHandler handler = (CommandHandler) applicationContext.getBean(
                command.getServiceName() + "CommandHandler");

        logger.info("Executing command: " + command.getClass().getSimpleName());
        return handler.handle(command);
    }
}
