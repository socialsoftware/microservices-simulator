package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;

public class SyncStep extends FlowStep {
    private Runnable syncOperation;
    
    public SyncStep(String stepName, Runnable syncOperation) {
        super(stepName);
        this.syncOperation = syncOperation;
    }

    // TODO temp
    public SyncStep(Runnable syncOperation) {
        this.syncOperation = syncOperation;
    }

    // TODO temp
    public SyncStep(Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        super(dependencies);
        this.syncOperation = syncOperation;
    }

    public SyncStep(String stepName, Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        super(stepName, dependencies);
        this.syncOperation = syncOperation;
    }

    /* 
    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        unitOfWork.registerCompensation(compensationLogic);
        syncOperation.run();
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        return result;
    }
    */

    
    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        try {
            if (getCompensation() != null) {
                ((SagaUnitOfWork)unitOfWork).registerCompensation(getCompensation());
            }
            syncOperation.run();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw e;
            //return CompletableFuture.failedFuture(e);
        }
    }
    
    
}