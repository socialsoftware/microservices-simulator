package com.example.dummyapp.shared.commandHandler;

import com.example.dummyapp.shared.commands.PingCommand;
import com.example.dummyapp.shared.service.SubstringTrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;

@Component
public class SubstringTrapCommandHandler extends CommandHandler {

    @Autowired
    private SubstringTrapService substringTrapService;

    @Override
    protected String getAggregateTypeName() {
        return "Shared";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case PingCommand cmd -> handlePing(cmd);
            default -> null;
        };
    }

    private Object handlePing(PingCommand cmd) {
        return substringTrapService.ping();
    }
}
