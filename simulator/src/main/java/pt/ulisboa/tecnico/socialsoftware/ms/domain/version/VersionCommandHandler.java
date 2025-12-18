
package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.DecrementVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.GetVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.IncrementVersionCommand;

import java.util.logging.Logger;

@Component
public class VersionCommandHandler {
    private static final Logger logger = Logger.getLogger(VersionCommandHandler.class.getName());

    @Autowired
    private IVersionService versionService;

    public Object handle(Command command) {
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
        try {
            return versionService.getVersionNumber();
        } catch (Exception e) {
            logger.severe("Failed to get version: " + e.getMessage());
            return e;
        }
    }

    private Object handleIncrementVersion() {
        logger.info("Incrementing version number");
        try {
            return versionService.incrementAndGetVersionNumber();
        } catch (Exception e) {
            logger.severe("Failed to increment version: " + e.getMessage());
            return e;
        }
    }

    private Object handleDecrementVersion() {
        logger.info("Decrementing version number");
        try {
            versionService.decrementVersionNumber();
            return null;
        } catch (Exception e) {
            logger.severe("Failed to decrement version: " + e.getMessage());
            return e;
        }
    }
}
