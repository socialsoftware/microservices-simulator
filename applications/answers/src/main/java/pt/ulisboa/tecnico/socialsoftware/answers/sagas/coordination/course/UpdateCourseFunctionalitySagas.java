package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto updatedCourseDto;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateCourseFunctionalitySagas(CourseService courseService, SagaUnitOfWorkService unitOfWorkService, Integer courseAggregateId, CourseDto courseDto, SagaUnitOfWork unitOfWork) {
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, courseDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, CourseDto courseDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateCourseStep = new SagaSyncStep("updateCourseStep", () -> {
            CourseDto updatedCourseDto = courseService.updateCourse(courseAggregateId, courseDto, unitOfWork);
            setUpdatedCourseDto(updatedCourseDto);
        });

        workflow.addStep(updateCourseStep);
    }

    public CourseDto getUpdatedCourseDto() {
        return updatedCourseDto;
    }

    public void setUpdatedCourseDto(CourseDto updatedCourseDto) {
        this.updatedCourseDto = updatedCourseDto;
    }
}
