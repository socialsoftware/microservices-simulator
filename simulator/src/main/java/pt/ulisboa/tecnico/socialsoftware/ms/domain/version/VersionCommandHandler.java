
package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.DecrementVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.GetVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.IncrementVersionCommand;

import java.util.logging.Logger;

@Component
public class VersionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(VersionCommandHandler.class.getName());

    @Autowired
    private IVersionService versionService;

    @Override
    protected String getAggregateTypeName() {
        return "Version";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        Object result;
        switch (command) {
            case GetVersionCommand ignored -> result = handleGetVersion();
            case IncrementVersionCommand ignored -> result = handleIncrementVersion();
            case DecrementVersionCommand ignored -> result = handleDecrementVersion();
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                result = null;
            }
        }
        return result;
    }

    private Object handleGetVersion() {
        logger.info("Getting version number");
        return versionService.getVersionNumber();
    }

    private Object handleIncrementVersion() {
        logger.info("Incrementing version number");
        return versionService.incrementAndGetVersionNumber();
    }

    private Object handleDecrementVersion() {
        logger.info("Decrementing version number");
        versionService.decrementVersionNumber();
        return null;
    }
}
