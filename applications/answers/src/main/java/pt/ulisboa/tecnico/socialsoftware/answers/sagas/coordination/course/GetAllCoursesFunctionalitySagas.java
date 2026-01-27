package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllCoursesFunctionalitySagas extends WorkflowFunctionality {
    private List<CourseDto> courses;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllCoursesFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CourseService courseService) {
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllCoursesStep = new SagaSyncStep("getAllCoursesStep", () -> {
            List<CourseDto> courses = courseService.getAllCourses();
            setCourses(courses);
        });

        workflow.addStep(getAllCoursesStep);

    }

    public List<CourseDto> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseDto> courses) {
        this.courses = courses;
    }
}
