package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class IncrementVersionCommand extends Command {
    public IncrementVersionCommand() {
        super(null, "version", null);
    }
}
