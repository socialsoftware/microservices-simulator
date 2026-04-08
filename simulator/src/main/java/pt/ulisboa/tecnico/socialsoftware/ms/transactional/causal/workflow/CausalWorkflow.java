package pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

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
