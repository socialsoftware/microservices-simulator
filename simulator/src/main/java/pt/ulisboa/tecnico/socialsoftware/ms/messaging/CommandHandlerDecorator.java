package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

public interface CommandHandlerDecorator {
    Object handle(Command command, CommandHandler serviceCommandHandler);
}
