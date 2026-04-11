
package pt.ulisboa.tecnico.socialsoftware.ms.versioning;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.DecrementVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.GetNextVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.GetVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.IncrementVersionCommand;

import java.util.logging.Logger;

@Component
@Profile("version-service")
public class VersionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(VersionCommandHandler.class.getName());

    @Autowired
    private IVersionService versionService;

    @Override
    public String getAggregateTypeName() {
        return "Version";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        Object result;
        switch (command) {
            case GetVersionCommand ignored -> result = handleGetVersion();
            case GetNextVersionCommand ignored -> result = handleGetNextVersion();
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

    private Object handleGetNextVersion() {
        logger.info("Getting next version number");
        return versionService.getNextVersionNumber();
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
