package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;

public abstract class Workflow {
    private UnitOfWorkService unitOfWorkService;
    private UnitOfWork unitOfWork;
    private WorkflowData data;
    private HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies = new HashMap<>();

    public Workflow(WorkflowData data, UnitOfWorkService unitOfWorkService, String functionalityName, UnitOfWork unitOfWork) {
        this.data = data;
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
    }

    public Workflow(UnitOfWorkService unitOfWorkService, String functionalityName, UnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
    }

    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public void addStep(FlowStep step){
        this.stepsWithDependencies.put(step, step.getDependencies());
    }

    public abstract ExecutionPlan planOrder(HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies);

    public CompletableFuture<Void> execute() {
        ExecutionPlan executionPlan = planOrder(this.stepsWithDependencies); // redefined for each transaction model

        return executionPlan.execute()
            .thenRun(() -> unitOfWorkService.commit(unitOfWork))
            .exceptionally(ex -> {
                unitOfWorkService.abort(unitOfWork);
                throw new RuntimeException(ex);
            });
    }
}
