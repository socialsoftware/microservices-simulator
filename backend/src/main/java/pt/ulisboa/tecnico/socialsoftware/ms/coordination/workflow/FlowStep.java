package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public abstract class FlowStep {
    private String stepName;
    private ArrayList<FlowStep> dependencies = new ArrayList<>();

    public FlowStep() {
    }

    public FlowStep(String stepName) {
        this.stepName = stepName;
    }

    public FlowStep(ArrayList<FlowStep> dependencies) {
        this.dependencies = dependencies;
    }

    public FlowStep(String stepName,  ArrayList<FlowStep> dependencies) {
        this.stepName = stepName;
        this.dependencies = dependencies;
    }

    public void setDependencies(ArrayList<FlowStep> dependencies) {
        this.dependencies = dependencies;
    }

    public ArrayList<FlowStep> getDependencies() {
        return this.dependencies;
    }

    public String getName() {
        return this.stepName;
    }

    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        return null;
    }
}
