package pt.ulisboa.tecnico.socialsoftware.ms.domain.version;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class GetVersionCommand extends Command {
    public GetVersionCommand() {
        super(null, "version", null);
    }
}
