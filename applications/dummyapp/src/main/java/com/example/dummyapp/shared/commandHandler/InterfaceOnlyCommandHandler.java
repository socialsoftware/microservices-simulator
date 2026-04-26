package com.example.dummyapp.shared.commandHandler;

import com.example.dummyapp.shared.commands.InterfaceOnlyCommand;
import com.example.dummyapp.shared.service.InterfaceOnlyServiceApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;

@Component
public class InterfaceOnlyCommandHandler extends CommandHandler {

    @Autowired
    private InterfaceOnlyServiceApi interfaceOnlyService;

    @Override
    protected String getAggregateTypeName() {
        return "InterfaceOnly";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case InterfaceOnlyCommand cmd -> handleInterfaceOnly(cmd);
            default -> null;
        };
    }

    private Object handleInterfaceOnly(InterfaceOnlyCommand cmd) {
        return interfaceOnlyService.loadInterfaceOnly(cmd.getId(), cmd.getUnitOfWork());
    }
}
