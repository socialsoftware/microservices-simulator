package pt.ulisboa.tecnico.socialsoftware.ms.versioning.command;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class GetVersionCommand extends Command {
    public GetVersionCommand() {
        super(null, "version", null);
    }
}
