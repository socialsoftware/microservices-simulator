package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.saga;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.CreateCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;

public class CreateCourseExecutionSaga extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateCourseExecutionSaga(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        buildWorkflow(unitOfWork);
    }

    private void buildWorkflow(SagaUnitOfWork unitOfWork) {
        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            new GetCourseByIdCommand(null, null, null);
        });
        SagaStep createCourseExecutionStep = new SagaStep("createCourseExecutionStep", () -> {
            new CreateCourseExecutionCommand(null, null, null);
        });
    }
}
