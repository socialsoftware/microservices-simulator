package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public class SyncStep implements FlowStep {
    private String stepName;
    private Runnable syncOperation;
    private Runnable compensationLogic;
    private ArrayList<FlowStep> dependencies = new ArrayList<>();

    public SyncStep(String stepName, Runnable syncOperation) {
        this.stepName = stepName;
        this.syncOperation = syncOperation;
    }

    //temp
    public SyncStep(Runnable syncOperation) {
        this.stepName = stepName;
        this.syncOperation = syncOperation;
    }

    //temp
    public SyncStep(Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        this.stepName = stepName;
        this.syncOperation = syncOperation;
        this.dependencies = dependencies;
    }

    public SyncStep(String stepName, Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        this.stepName = stepName;
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

    /*
    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        try {
            unitOfWork.registerCompensation(compensationLogic);
            syncOperation.run();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
    */

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

    @Override
    public String getName() {
        return this.stepName;
    }
}