package pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;

public class CausalWorkflow extends Workflow {
    public CausalWorkflow(WorkflowFunctionality functionality, UnitOfWorkService unitOfWorkService, CausalUnitOfWork unitOfWork) {
        super(functionality, unitOfWorkService, unitOfWork);
    }

    @Override
    public ExecutionPlan planOrder(HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies) {
        ArrayList<FlowStep> orderedSteps = new ArrayList<>();

        for (HashMap.Entry<FlowStep, ArrayList<FlowStep>> entry: stepsWithDependencies.entrySet()) {
            orderedSteps.add(entry.getKey());
        }

        return new ExecutionPlan(orderedSteps, stepsWithDependencies, this.getFunctionality());
    }

    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        ExecutionPlan executionPlan = planOrder(this.stepsWithDependencies);

        // Initialize futures for steps with no dependencies
        for (FlowStep step: executionPlan.getPlan()) {
            step.execute(unitOfWork);
        }
        unitOfWorkService.commit(unitOfWork);
        return CompletableFuture.completedFuture(null);
    }
}
