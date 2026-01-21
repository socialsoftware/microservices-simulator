package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.local;

import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

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
        Object returnObject = handler.handle(command);
        if (returnObject instanceof SimulatorException) {
            throw (SimulatorException) returnObject;
        } else if (returnObject instanceof TransientDataAccessException) {
            throw (TransientDataAccessException) returnObject;
        }
        return returnObject;
    }
}
