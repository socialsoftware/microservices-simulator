package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

class TestFunctionality extends WorkflowFunctionality {

    TestFunctionality(
            List<FlowStep> steps,
            SagaUnitOfWorkService unitOfWorkService,
            SagaUnitOfWork unitOfWork) {

        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        for (FlowStep step : steps) {
            this.workflow.addStep(step);
        }
    }
}
