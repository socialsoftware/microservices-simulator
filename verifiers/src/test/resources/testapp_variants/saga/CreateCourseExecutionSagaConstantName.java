package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp_variants.saga;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;

// W2: The step name argument is a NameExpr (constant reference) instead of a StringLiteralExpr.
// WorkflowFunctionalityVisitor calls ifStringLiteralExpr() on the first argument, which only
// fires for literal strings. A NameExpr argument is silently ignored, so the entire SagaStep
// construction is skipped and the saga's step list remains empty.
public class CreateCourseExecutionSagaConstantName extends WorkflowFunctionality {

    private static final String GET_COURSE_STEP = "getCourseStep";

    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateCourseExecutionSagaConstantName(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        buildWorkflow(unitOfWork);
    }

    private void buildWorkflow(SagaUnitOfWork unitOfWork) {
        SagaStep getCourseStep = new SagaStep(GET_COURSE_STEP, () -> {
            new GetCourseByIdCommand(null, null, null);
        });
    }
}
