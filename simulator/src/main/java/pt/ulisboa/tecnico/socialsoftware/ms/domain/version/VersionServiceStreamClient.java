package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.DecrementVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.GetVersionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command.IncrementVersionCommand;

@Service
@Profile("stream")
public class VersionServiceStreamClient implements IVersionService {
    private static final Logger logger = LoggerFactory.getLogger(VersionServiceStreamClient.class);

    private final CommandGateway commandGateway;

    public VersionServiceStreamClient(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public Integer getVersionNumber() {
        logger.debug("Requesting version number via CommandGateway");
        return (Integer) commandGateway.send(new GetVersionCommand());
    }

    @Override
    public Integer incrementAndGetVersionNumber() {
        logger.debug("Requesting increment version via CommandGateway");
        return (Integer) commandGateway.send(new IncrementVersionCommand());
    }

    @Override
    public void decrementVersionNumber() {
        logger.debug("Requesting decrement version");
        commandGateway.send(new DecrementVersionCommand());
    }
}
