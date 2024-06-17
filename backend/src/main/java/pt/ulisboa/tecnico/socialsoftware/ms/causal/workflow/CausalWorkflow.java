package pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow;

import java.util.ArrayList;
import java.util.HashMap;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Workflow;

public class CausalWorkflow extends Workflow {
    public CausalWorkflow(UnitOfWorkService unitOfWorkService, String functionalityName) {
        super(unitOfWorkService, functionalityName);
    }

    @Override
    public ExecutionPlan planOrder(HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies) {
        //TODO
        return null;
    }
}
