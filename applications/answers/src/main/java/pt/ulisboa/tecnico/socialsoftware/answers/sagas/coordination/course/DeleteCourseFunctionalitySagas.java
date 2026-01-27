package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteCourseFunctionalitySagas extends WorkflowFunctionality {
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteCourseFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CourseService courseService, Integer courseAggregateId) {
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteCourseStep = new SagaSyncStep("deleteCourseStep", () -> {
            courseService.deleteCourse(courseAggregateId);
        });

        workflow.addStep(deleteCourseStep);

    }

}
