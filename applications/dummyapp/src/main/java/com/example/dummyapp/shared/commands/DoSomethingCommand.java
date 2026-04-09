package com.example.dummyapp.shared.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class DoSomethingCommand extends Command {

    private final Integer id;

    public DoSomethingCommand(UnitOfWork unitOfWork, String serviceName, Integer id) {
        super(unitOfWork, serviceName, id);
        this.id = id;
    }

    public Integer getId() { return id; }
}
