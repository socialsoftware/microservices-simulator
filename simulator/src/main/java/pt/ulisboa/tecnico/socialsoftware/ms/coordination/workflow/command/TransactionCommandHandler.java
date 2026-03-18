package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

public interface TransactionCommandHandler {
    Object handle(Command command, CommandHandler serviceCommandHandler);
}
