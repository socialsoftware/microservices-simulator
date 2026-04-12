package com.example.dummyapp.shared.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import com.example.dummyapp.shared.commands.DoSomethingCommand;
import com.example.dummyapp.shared.service.AmbiguousServiceApi;

@Component
public class AmbiguousCommandHandler extends CommandHandler {

    @Autowired
    private AmbiguousServiceApi ambiguousService;

    @Override
    protected String getAggregateTypeName() {
        return "Ambiguous";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case DoSomethingCommand cmd -> handleDoSomething(cmd);
            default -> null;
        };
    }

    private Object handleDoSomething(DoSomethingCommand cmd) {
        return ambiguousService.doSomething(cmd.getId(), cmd.getUnitOfWork());
    }
}
