package pt.ulisboa.tecnico.socialsoftware.ms.version.command;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class DecrementVersionCommand extends Command {
    public DecrementVersionCommand() {
        super(null, "version", null);
    }
}
