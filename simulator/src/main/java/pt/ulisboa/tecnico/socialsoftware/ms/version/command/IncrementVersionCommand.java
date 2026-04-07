package pt.ulisboa.tecnico.socialsoftware.ms.version.command;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class IncrementVersionCommand extends Command {
    public IncrementVersionCommand() {
        super(null, "version", null);
    }
}
