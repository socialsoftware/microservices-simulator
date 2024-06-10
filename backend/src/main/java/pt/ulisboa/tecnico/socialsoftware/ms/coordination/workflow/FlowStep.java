package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

import java.util.concurrent.Future;

public interface FlowStep {
    public Future<Void> execute();
    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork);
}
