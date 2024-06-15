package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;

public class SagaWorkflow extends Workflow {
    public SagaWorkflow(WorkflowData data, UnitOfWorkService unitOfWorkService, String functionalityName) {
        super(data, unitOfWorkService, functionalityName);
    }

    @Override
    public ExecutionPlan planOrder() {
        //TODO
        return null;
    }
}
