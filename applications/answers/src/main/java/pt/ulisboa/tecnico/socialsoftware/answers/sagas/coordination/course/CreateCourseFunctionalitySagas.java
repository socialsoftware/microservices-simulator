package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateCourseFunctionalitySagas extends WorkflowFunctionality {
    private CourseDto createdCourseDto;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateCourseFunctionalitySagas(CourseService courseService, SagaUnitOfWorkService unitOfWorkService, CourseDto courseDto, SagaUnitOfWork unitOfWork) {
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseDto, unitOfWork);
    }

    public void buildWorkflow(CourseDto courseDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createCourseStep = new SagaSyncStep("createCourseStep", () -> {
            CourseDto createdCourseDto = courseService.createCourse(courseDto, unitOfWork);
            setCreatedCourseDto(createdCourseDto);
        });

        workflow.addStep(createCourseStep);
    }

    public CourseDto getCreatedCourseDto() {
        return createdCourseDto;
    }

    public void setCreatedCourseDto(CourseDto createdCourseDto) {
        this.createdCourseDto = createdCourseDto;
    }
}
