package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

public interface CommandHandler {
    Object handle(Command command);
}
