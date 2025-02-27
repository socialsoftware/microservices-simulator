package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public interface SagaStep {
    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork);
    public Runnable getCompensation();
}
