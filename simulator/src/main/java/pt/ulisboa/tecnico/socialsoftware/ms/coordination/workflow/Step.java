package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

// TODO this will become Step
public class Step extends FlowStep {
    private final Runnable syncOperation;
    
    public Step(String stepName, Runnable syncOperation) {
        super(stepName);
        this.syncOperation = syncOperation;
    }

    public Step(Runnable syncOperation) {
        this.syncOperation = syncOperation;
    }
    
    public Step(Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        super(dependencies);
        this.syncOperation = syncOperation;
    }

    public Step(String stepName, Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        super(stepName, dependencies);
        this.syncOperation = syncOperation;
    }

    public Runnable getSyncOperation() {
        return this.syncOperation;
    }
    
    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        syncOperation.run();
        return CompletableFuture.completedFuture(null);
    }
    
    
}