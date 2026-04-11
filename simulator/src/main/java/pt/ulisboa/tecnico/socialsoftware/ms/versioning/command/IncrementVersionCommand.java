package pt.ulisboa.tecnico.socialsoftware.ms.versioning.command;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class IncrementVersionCommand extends Command {
    public IncrementVersionCommand() {
        super(null, "version", null);
    }
}
