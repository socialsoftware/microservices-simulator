package com.example.dummyapp.shared.commands;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public class InterfaceOnlyCommand extends Command {

    private final Integer id;

    public InterfaceOnlyCommand(UnitOfWork unitOfWork, String serviceName, Integer id) {
        super(unitOfWork, serviceName, id);
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
