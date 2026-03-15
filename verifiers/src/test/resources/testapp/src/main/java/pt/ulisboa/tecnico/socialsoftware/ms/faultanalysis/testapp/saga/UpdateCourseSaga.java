package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.saga;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;

public class UpdateCourseSaga extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateCourseSaga(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        buildWorkflow(unitOfWork);
    }

    private void buildWorkflow(SagaUnitOfWork unitOfWork) {
        SagaStep getCourseExecutionStep = new SagaStep("getCourseExecutionStep", () -> {
            new GetCourseExecutionByIdCommand(null, null, null);
        });
        SagaStep updateCourseStep = new SagaStep("updateCourseStep", () -> {
            new CreateCourseCommand(null, null, null);
        });
    }
}
