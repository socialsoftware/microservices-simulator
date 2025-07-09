package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.stereotype.Service;

public class CommandGateway {
    private static CommandGateway instance;

    public static  CommandGateway getInstance() {
        if (CommandGateway.instance == null) { CommandGateway.instance = new CommandGateway(); }
        return  CommandGateway.instance;
    }

    private CommandGateway() {}

    public void send(Command command) {
        command.execute();
    }
}
