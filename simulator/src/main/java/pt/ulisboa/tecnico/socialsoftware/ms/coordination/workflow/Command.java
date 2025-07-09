package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public abstract class Command {
    protected UnitOfWork unitOfWork;

    public Command(UnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public abstract void execute();

    public void undo() {
    }
}
