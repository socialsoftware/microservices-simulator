package pt.ulisboa.tecnico.socialsoftware.ms.domain.version.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetVersionCommand extends Command {
    public GetVersionCommand() {
        super(null, "version", null);
    }
}
