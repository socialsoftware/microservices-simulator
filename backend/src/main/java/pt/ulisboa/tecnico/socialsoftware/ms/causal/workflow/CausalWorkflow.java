package pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;

public class CausalWorkflow extends Workflow {
    public CausalWorkflow(WorkflowData data, UnitOfWorkService unitOfWorkService, String functionalityName) {
        super(data, unitOfWorkService, functionalityName);
    }

    @Override
    public ExecutionPlan planOrder() {
        //TODO
        return null;
    }
}
