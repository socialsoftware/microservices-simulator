package pt.ulisboa.tecnico.socialsoftware.ms.versioning.command;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;

public class GetNextVersionCommand extends Command {
	public GetNextVersionCommand() {
		super(null, "version", null);
	}
}
