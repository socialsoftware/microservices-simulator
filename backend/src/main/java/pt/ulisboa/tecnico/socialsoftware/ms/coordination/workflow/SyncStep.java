package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

import java.util.concurrent.CompletableFuture;

public class SyncStep implements FlowStep {
    private Runnable syncOperation;
    private Runnable compensationLogic;

    public SyncStep(Runnable syncOperation) {
        this.syncOperation = syncOperation;
    }

    @Override
    public CompletableFuture<Void> execute() {
        syncOperation.run();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork) {
        this.compensationLogic = compensationLogic;
        unitOfWork.registerCompensation(compensationLogic);
    }
}