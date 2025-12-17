package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;
import java.util.List;

public class SearchCoursesFunctionalitySagas extends WorkflowFunctionality {
    private List<CourseDto> searchedCourseDtos;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public SearchCoursesFunctionalitySagas(CourseService courseService, SagaUnitOfWorkService unitOfWorkService, String name, CourseType type, SagaUnitOfWork unitOfWork) {
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(name, type, unitOfWork);
    }

    public void buildWorkflow(String name, CourseType type, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchCoursesStep = new SagaSyncStep("searchCoursesStep", () -> {
            List<CourseDto> searchedCourseDtos = courseService.searchCourses(name, type, unitOfWork);
            setSearchedCourseDtos(searchedCourseDtos);
        });

        workflow.addStep(searchCoursesStep);
    }

    public List<CourseDto> getSearchedCourseDtos() {
        return searchedCourseDtos;
    }

    public void setSearchedCourseDtos(List<CourseDto> searchedCourseDtos) {
        this.searchedCourseDtos = searchedCourseDtos;
    }
}
