package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

public interface TransactionCommandHandler {
    Object handle(Command command, CommandHandler serviceCommandHandler);
}
