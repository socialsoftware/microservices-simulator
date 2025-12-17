package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class DeleteCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto deletedCourseDto;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public DeleteCourseFunctionalitySagas(CourseService courseService, SagaUnitOfWorkService unitOfWorkService, Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseStep = new SagaSyncStep("getCourseStep", () -> {
            CourseDto deletedCourseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            setDeletedCourseDto(deletedCourseDto);
            unitOfWorkService.registerSagaState(courseAggregateId, CourseSagaState.READ_COURSE, unitOfWork);
        });

        getCourseStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(courseAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep deleteCourseStep = new SagaSyncStep("deleteCourseStep", () -> {
            courseService.deleteCourse(courseAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(deleteCourseStep);
    }

    public CourseDto getDeletedCourseDto() {
        return deletedCourseDto;
    }

    public void setDeletedCourseDto(CourseDto deletedCourseDto) {
        this.deletedCourseDto = deletedCourseDto;
    }
}
