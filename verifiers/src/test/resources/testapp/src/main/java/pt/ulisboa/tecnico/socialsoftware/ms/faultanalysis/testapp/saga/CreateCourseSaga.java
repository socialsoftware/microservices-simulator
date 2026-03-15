package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.saga;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.command.CreateCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.dto.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;

/**
 * A saga with both infrastructure and domain constructor arguments,
 * used to test SpockTestVisitor input extraction with variable dependency tracing.
 */
public class CreateCourseSaga extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateCourseSaga(SagaUnitOfWorkService unitOfWorkService,
                            Integer courseAggregateId,
                            CourseDto courseDto,
                            SagaUnitOfWork unitOfWork) {
        this.unitOfWorkService = unitOfWorkService;
        buildWorkflow(unitOfWork);
    }

    private void buildWorkflow(SagaUnitOfWork unitOfWork) {
        SagaStep createCourseStep = new SagaStep("createCourseStep", () -> {
            new CreateCourseCommand(null, null, null);
        });
    }
}
