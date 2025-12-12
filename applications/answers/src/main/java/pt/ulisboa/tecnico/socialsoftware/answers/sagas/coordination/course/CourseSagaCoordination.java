package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class CourseSagaCoordination extends WorkflowFunctionality {
private CourseDto courseDto;
private SagaCourseDto course;
private final CourseService courseService;
private final SagaUnitOfWorkService unitOfWorkService;

public CourseSagaCoordination(CourseService courseService, SagaUnitOfWorkService
unitOfWorkService,
CourseDto courseDto, SagaUnitOfWork unitOfWork) {
this.courseService = courseService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(courseDto, unitOfWork);
}

public void buildWorkflow(CourseDto courseDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public CourseDto getCourseDto() {
return courseDto;
}

public void setCourseDto(CourseDto courseDto) {
this.courseDto = courseDto;
}

public SagaCourseDto getCourse() {
return course;
}

public void setCourse(SagaCourseDto course) {
this.course = course;
}
}