package pt.ulisboa.tecnico.socialsoftware.ms.versioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.DecrementVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.GetNextVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.GetVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.command.IncrementVersionCommand;

@Service
@Profile("remote & !version-service & !distributed-version")
public class VersionServiceClient implements IVersionService {
    private static final Logger logger = LoggerFactory.getLogger(VersionServiceClient.class);

    private final CommandGateway commandGateway;

    public VersionServiceClient(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public Long getVersionNumber() {
        logger.debug("Requesting version number via CommandGateway");
        return (Long) commandGateway.send(new GetVersionCommand());
    }

    @Override
    public Long getNextVersionNumber() {
        logger.debug("Requesting next version number via CommandGateway");
        return (Long) commandGateway.send(new GetNextVersionCommand());
    }

    @Override
    public Long incrementAndGetVersionNumber() {
        logger.debug("Requesting increment version via CommandGateway");
        return (Long) commandGateway.send(new IncrementVersionCommand());
    }

    @Override
    public void decrementVersionNumber() {
        logger.debug("Requesting decrement version");
        commandGateway.send(new DecrementVersionCommand());
    }
}
