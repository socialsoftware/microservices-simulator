package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp_variants.saga;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;

// W1: The step action is passed as a method reference (this::executeGetCourse) instead of
// a lambda expression. WorkflowFunctionalityVisitor calls ifLambdaExpr() which only fires
// for LambdaExpr nodes. A MethodReferenceExpr is silently ignored, so the step is registered
// but has zero footprints.
public class CreateCourseExecutionSagaMethodRef extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateCourseExecutionSagaMethodRef(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        buildWorkflow(unitOfWork);
    }

    private void buildWorkflow(SagaUnitOfWork unitOfWork) {
        SagaStep getCourseStep = new SagaStep("getCourseStep", this::executeGetCourse);
    }

    private void executeGetCourse() {
        new GetCourseByIdCommand(null, null, null);
    }
}
