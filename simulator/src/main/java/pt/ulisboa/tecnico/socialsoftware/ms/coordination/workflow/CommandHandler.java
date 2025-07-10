package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

public interface CommandHandler<C extends Command> {
    void handle(C command);
}
