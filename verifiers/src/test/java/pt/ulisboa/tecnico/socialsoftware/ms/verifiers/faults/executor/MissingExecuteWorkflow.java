package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class MissingExecuteWorkflow {
    public static int compensationCalls = 0;

    public MissingExecuteWorkflow(Object argument) {
    }

    public void resumeWorkflow(UnitOfWork unitOfWork) {
    }

    public void resumeCompensation(UnitOfWork unitOfWork) {
        compensationCalls++;
    }
}
