package pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class DecrementVersionCommand extends Command {
    public DecrementVersionCommand() {
        super(null, "version", null);
    }
}
