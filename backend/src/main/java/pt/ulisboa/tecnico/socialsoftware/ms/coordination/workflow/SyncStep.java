package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public class SyncStep implements FlowStep {
    private Runnable syncOperation;
    private Runnable compensationLogic;
    private ArrayList<FlowStep> dependencies = new ArrayList<>();

    public SyncStep(Runnable syncOperation) {
        this.syncOperation = syncOperation;
    }

    public SyncStep(Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        this.syncOperation = syncOperation;
        this.dependencies = dependencies;
    }

    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        unitOfWork.registerCompensation(compensationLogic);
        syncOperation.run();
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);
        return result;
    }

    @Override
    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork) {
        this.compensationLogic = compensationLogic;
    }

    @Override
    public ArrayList<FlowStep> getDependencies() {
        return this.dependencies;
    }

    @Override
    public Runnable getCompensation() {
        return this.compensationLogic;
    }

    @Override
    public void setDependencies(ArrayList<FlowStep> dependencies) {
        this.dependencies = dependencies;
    }
}